package org.akhq;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.salesforce.kafka.test.KafkaBrokers;
import com.salesforce.kafka.test.KafkaProvider;
import com.salesforce.kafka.test.KafkaTestUtils;
import com.salesforce.kafka.test.ListenerProperties;
import com.yammer.metrics.core.Stoppable;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.akhq.clusters.EmbeddedSingleNodeKafkaCluster;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
public class KafkaTestCluster implements Runnable, Stoppable {
    private static final Path CS_PATH = Paths.get(System.getProperty("java.io.tmpdir"), "/akhq-cs.json");

    public static final String CLUSTER_ID = "test";

    public static final String TOPIC_RANDOM = "random";
    public static final String TOPIC_TOBE_EMPTIED = "emptied";
    public static final String TOPIC_COMPACTED = "compacted";
    public static final String TOPIC_EMPTY = "empty";
    public static final String TOPIC_HUGE = "huge";
    public static final String TOPIC_STREAM_IN = "stream-in";
    public static final String TOPIC_STREAM_MAP = "stream-map";
    public static final String TOPIC_STREAM_COUNT = "stream-count";
    public static final String TOPIC_CONNECT = "connect-sink";

    public static final int TOPIC_ALL_COUNT = 19;
    public static final int TOPIC_HIDE_INTERNAL_COUNT = 11;
    public static final int TOPIC_HIDE_INTERNAL_STREAM_COUNT = 9;
    public static final int TOPIC_HIDE_STREAM_COUNT = 17;
    // github actions says 11 ...
    public static final int CONSUMER_GROUP_COUNT = 11;

    public static final String CONSUMER_STREAM_TEST = "stream-test-example";

    private EmbeddedSingleNodeKafkaCluster kafkaCluster;
    private KafkaTestUtils testUtils;
    private boolean reuse;
    private ConnectionString connectionString;
    private StreamTest stream;

    public static void main(String[] args) throws Exception {
        List<String> argsList = Arrays.asList(args);

        if (argsList.size() > 0 && argsList.get(0).equals("inject")) {
            KafkaTestCluster kafkaTestCluster = new KafkaTestCluster();
            kafkaTestCluster.injectTestData();
        } else {
            KafkaTestCluster kafkaTestCluster = new KafkaTestCluster(true);
            kafkaTestCluster.run();
        }
    }

    /**
     * Reuse a local broker for inject only
     */
    private KafkaTestCluster() {
        this.connectionString = new ConnectionString.ConnectionStringBuilder()
            .schemaRegistry("http://schema-registry:8085")
            .kafka("kafka:9092")
            .connect1("http://connect:8083")
            .connect2("http://connect:8084")
            .build();

        testUtils = new KafkaTestUtils(new Provider(this.connectionString));
    }

    public KafkaTestCluster(boolean reuseEnabled) throws Exception {
        reuse = reuseEnabled;

        kafkaCluster = new EmbeddedSingleNodeKafkaCluster(new Properties() {{
            // Log config
            put("log.min.cleanable.dirty.ratio", "0");
            put("log.roll.ms", "1");
            put("log.cleaner.backoff.ms", "1");
            put("log.segment.delete.delay.ms", "1");
            put("max.compaction.lag.ms", "1");
            put("authorizer.class.name", "kafka.security.auth.SimpleAclAuthorizer");
            put("allow.everyone.if.no.acl.found", "true");

            // Segment config
            put(TopicConfig.SEGMENT_MS_CONFIG, "1");
            put(TopicConfig.SEGMENT_INDEX_BYTES_CONFIG, "1");
            put(TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG, "0.01");
            put(TopicConfig.DELETE_RETENTION_MS_CONFIG, "1");

            // Lower active threads.
            put("num.io.threads", "2");
            put("num.network.threads", "2");
            put("log.flush.interval.messages", "1");

            // Define replication factor for internal topics to 1
            put("offsets.topic.replication.factor", "1");
            put("offset.storage.replication.factor", "1");
            put("transaction.state.log.replication.factor", "1");
            put("transaction.state.log.min.isr", "1");
            put("transaction.state.log.num.partitions", "4");
            put("config.storage.replication.factor", "1");
            put("status.storage.replication.factor", "1");
            put("default.replication.factor", "1");
        }});

        kafkaCluster.start();
    }

    @Override
    public void stop() {
        kafkaCluster.stop();
        stream.stop();
    }

    @Override
    public void run() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        try {
            kafkaCluster.start();
            log.info("Kafka Server started on {}", kafkaCluster.bootstrapServers());
            log.info("Kafka Schema registry started on {}", kafkaCluster.schemaRegistryUrl());
            log.info("Kafka Connect started on {}", kafkaCluster.connect1Url());
            log.info("Kafka Connect started on {}", kafkaCluster.connect2Url());

            connectionString = ConnectionString.builder()
                .kafka(kafkaCluster.bootstrapServers())
                .zookeeper(kafkaCluster.zookeeperConnect())
                .schemaRegistry(kafkaCluster.schemaRegistryUrl())
                .connect1(kafkaCluster.connect1Url())
                .connect2(kafkaCluster.connect2Url())
                .build();

            testUtils = new KafkaTestUtils(new Provider(this.connectionString));

            if (reuse) {
                writeClusterInfo();
            }

            injectTestData();
            log.info("Test data injected");

            Thread.sleep(5000);
            log.info("Test data injected sleep done");

            if (reuse) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        Files.delete(CS_PATH);
                    } catch (Exception e) {
                        log.error("Can't delete CS file", e);
                    }
                }));
            }
        } catch (Exception  e) {
            throw new RuntimeException(e);
        }
    }

    public static ConnectionString readClusterInfo() throws IOException {
        if (!Files.exists(CS_PATH)) {
            return null;
        }

        return new Gson().fromJson(new String(Files.readAllBytes(CS_PATH)), ConnectionString.class);
    }

    public ConnectionString getClusterInfo() {
        return connectionString;
    }

    private void writeClusterInfo() throws IOException {
        Files.write(
            CS_PATH,
            new GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(connectionString)
                .getBytes()
        );
    }

    private void injectTestData() throws InterruptedException, ExecutionException {
        // stream
        testUtils.createTopic(TOPIC_STREAM_IN, 3, (short) 1);
        testUtils.createTopic(TOPIC_STREAM_MAP, 3, (short) 1);
        testUtils.createTopic(TOPIC_STREAM_COUNT, 3, (short) 1);
        stream = new StreamTest(this.connectionString.getKafka(), this.connectionString.getSchemaRegistry());
        stream.run();

        testUtils.produceRecords(
            ImmutableMap.<byte[], byte[]>builder()
                .put("1".getBytes(), "1;WaWa;ABYSSINIAN".getBytes())
                .put("2".getBytes(), "2;Romeo;AMERICAN_SHORTHAIR".getBytes())
                .put("3".getBytes(), "3;Matisse;BIRMAN".getBytes())
                .put("4".getBytes(), "4;Finnegan;MAINE_COON".getBytes())
                .put("5".getBytes(), "5;Forrest;ORIENTAL".getBytes())
                .put("6".getBytes(), "6;Edgar;PERSIAN".getBytes())
                .put("7".getBytes(), "7;Desmond;RAGDOLL".getBytes())
                .put("8".getBytes(), "8;Darcy;SIAMESE".getBytes())
                .put("9".getBytes(), "9;Byron;SPHYNX".getBytes())
                .put("10".getBytes(), "10;Augustus;ABYSSINIAN".getBytes())
                .put("11".getBytes(), "11;Arturo;ABYSSINIAN".getBytes())
                .put("12".getBytes(), "12;Archibald;RAGDOLL".getBytes())
                .build(),
            TOPIC_STREAM_IN,
            1
        );
        log.debug("Stream started");

        // compacted topic
        testUtils.createTopic(TOPIC_COMPACTED, 3, (short) 1);
        testUtils.getAdminClient().alterConfigs(ImmutableMap.of(
            new ConfigResource(ConfigResource.Type.TOPIC, TOPIC_COMPACTED),
            new Config(List.of(
                new ConfigEntry(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT),
                new ConfigEntry(TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG, "0"),
                new ConfigEntry(TopicConfig.MAX_COMPACTION_LAG_MS_CONFIG, "1")
            ))
        )).all().get();

        KafkaProducer<String, String> producer = testUtils.getKafkaProducer(
            StringSerializer.class,
            StringSerializer.class
        );

        for (int partition = 0; partition < 3; partition++) {
            for (int count = 0; count < 50; count++) {
                producer.send(new ProducerRecord<>(TOPIC_COMPACTED, partition, "compact-key", "Partition(" + partition + ") Count(" + count + ")")).get();
                Thread.sleep(10L);
            }
            Thread.sleep(10L);
            testUtils.produceRecords(randomDatas(50, 0), TOPIC_COMPACTED, partition);
        }
        log.debug("Compacted topic created");

        // empty topic
        testUtils.createTopic(TOPIC_EMPTY, 12, (short) 1);
        log.debug("Empty topic created");

        // empty topic
        testUtils.createTopic(TOPIC_CONNECT, 12, (short) 1);
        log.debug("Connect topic created");

        // random data
        testUtils.createTopic(TOPIC_RANDOM, 3, (short) 1);
        for (int partition = 0; partition < 3; partition++) {
            testUtils.produceRecords(randomDatas(100, 0), TOPIC_RANDOM, partition);
        }
        log.debug("Random topic created");

        // random data to be emptied
        testUtils.createTopic(TOPIC_TOBE_EMPTIED, 3, (short) 1);
        for (int partition = 0; partition < 3; partition++) {
            testUtils.produceRecords(randomDatas(100, 0), TOPIC_TOBE_EMPTIED, partition);
        }
        log.debug("Random topic to be emptied created");

        // huge data
        testUtils.createTopic(TOPIC_HUGE, 3, (short) 1);
        for (int partition = 0; partition < 3; partition++) {
            testUtils.produceRecords(randomDatas(1000, 0), TOPIC_HUGE, partition);
        }
        log.debug("Huge topic created");

        // consumer groups
        for (int c = 0; c < 5; c++) {
            Properties properties = new Properties();
            properties.put("group.id", "consumer-" + c);

            KafkaConsumer<String, String> consumer = testUtils.getKafkaConsumer(StringDeserializer.class, StringDeserializer.class, properties);
            consumer.subscribe(Collections.singletonList(KafkaTestCluster.TOPIC_COMPACTED));
            consumer.poll(Duration.ofMillis(1000));
            consumer.commitSync();
            consumer.close();
        }
        log.debug("Consumers created");

        // acls
        List<AclBinding> bindings = new ArrayList<>();
        bindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TOPIC, "testAclTopic", PatternType.LITERAL),
                new AccessControlEntry("user:toto", "*", AclOperation.DESCRIBE, AclPermissionType.ALLOW))
        );
        bindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TOPIC, "testAcl", PatternType.PREFIXED),
                new AccessControlEntry("user:toto", "*", AclOperation.READ, AclPermissionType.DENY))
        );
        bindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TOPIC, "testAclTopic", PatternType.LITERAL),
                new AccessControlEntry("user:tata", "my-host", AclOperation.WRITE, AclPermissionType.ALLOW))
        );
        bindings.add(new AclBinding(
                new ResourcePattern(ResourceType.TOPIC, "anotherAclTestTopic", PatternType.LITERAL),
                new AccessControlEntry("user:toto", "*", AclOperation.DESCRIBE, AclPermissionType.ALLOW))
        );
        bindings.add(new AclBinding(
                new ResourcePattern(ResourceType.GROUP, "groupConsumer", PatternType.LITERAL),
                new AccessControlEntry("user:toto", "*", AclOperation.DESCRIBE, AclPermissionType.ALLOW))
        );
        bindings.add(new AclBinding(
                new ResourcePattern(ResourceType.GROUP, "groupConsumer", PatternType.LITERAL),
                new AccessControlEntry("user:tata", "*", AclOperation.DESCRIBE, AclPermissionType.ALLOW))
        );
        bindings.add(new AclBinding(
                new ResourcePattern(ResourceType.GROUP, "groupConsumer2", PatternType.LITERAL),
                new AccessControlEntry("user:toto", "*", AclOperation.DESCRIBE, AclPermissionType.ALLOW))
        );
        testUtils.getAdminClient().createAcls(bindings).all().get();
        log.debug("bindings acls added");
    }

    private static Map<byte[], byte[]> randomDatas(int size, Integer start) {
        final Map<byte[], byte[]> keysAndValues = new HashMap<>();

        for (int j = (start != null ? start : 0); j < (start != null ? start + size : size); j++) {
            final String key = "key_" + j;
            final String value = "value_" + j;

            keysAndValues.put(key.getBytes(Charsets.UTF_8), value.getBytes(Charsets.UTF_8));
        }

        return keysAndValues;
    }

    private static class Provider implements KafkaProvider {
        private final ConnectionString connectionString;

        public Provider(ConnectionString connection) {
            connectionString = connection;
        }

        @Override
        public KafkaBrokers getKafkaBrokers() {
            return null;
        }

        @Override
        public String getKafkaConnectString() {
            return connectionString.getKafka();
        }

        @Override
        public List<ListenerProperties> getListenerProperties() {
            return Collections.singletonList(new ListenerProperties(
                "PLAINTEXT",
                connectionString.getKafka(),
                new Properties()
            ));
        }

        @Override
        public String getZookeeperConnectString() {
            return connectionString.getZookeeper();
        }
    }

    @Builder
    @Getter
    public static class ConnectionString {
        private final String kafka;
        private final String zookeeper;
        private final String schemaRegistry;
        private final String connect1;
        private final String connect2;
    }
}
