package org.kafkahq;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.salesforce.kafka.test.KafkaBrokers;
import com.salesforce.kafka.test.KafkaProvider;
import com.salesforce.kafka.test.KafkaTestUtils;
import com.salesforce.kafka.test.ListenerProperties;
import com.yammer.metrics.core.Stoppable;
import io.confluent.kafka.schemaregistry.RestApp;
import io.confluent.kafka.schemaregistry.avro.AvroCompatibilityLevel;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig;
import lombok.Builder;
import lombok.Getter;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.kafkahq.repositories.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class KafkaTestCluster implements Runnable, Stoppable {
    private static final Path CS_PATH = Paths.get(System.getProperty("java.io.tmpdir"), "/kafkahq-cs.json");

    public static final String CLUSTER_ID = "test";

    public static final String TOPIC_RANDOM = "random";
    public static final String TOPIC_COMPACTED = "compacted";
    public static final String TOPIC_EMPTY = "empty";
    public static final String TOPIC_HUGE = "huge";
    public static final String TOPIC_STREAM_IN = "stream-in";
    public static final String TOPIC_STREAM_MAP = "stream-map";
    public static final String TOPIC_STREAM_COUNT = "stream-count";

    private static Logger logger = LoggerFactory.getLogger(RecordRepository.class);
    private short numberOfBrokers;
    private boolean reuse;
    private com.salesforce.kafka.test.KafkaTestCluster cluster;
    private RestApp schemaRegistry;
    private KafkaTestUtils testUtils;
    private ReuseFile reuseFile;
    private StreamTest stream;

    public com.salesforce.kafka.test.KafkaTestCluster getCluster() {
        return cluster;
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        List<String> argsList = Arrays.asList(args);

        if (argsList.size() > 0 && argsList.get(0).equals("inject")) {
            KafkaTestCluster kafkaTestCluster = new KafkaTestCluster();
            kafkaTestCluster.injectTestData();
        } else {
            KafkaTestCluster kafkaTestCluster = new KafkaTestCluster((short) 3, true);
            kafkaTestCluster.run();
        }
    }

    /**
     * Reuse a local broker
     */
    private KafkaTestCluster() {
        this.numberOfBrokers = 1;
        this.testUtils = new KafkaTestUtils(new KafkaProvider() {
            @Override
            public KafkaBrokers getKafkaBrokers() {
                return null;
            }

            @Override
            public String getKafkaConnectString() {
                return "kafka:9092";
            }

            @Override
            public List<ListenerProperties> getListenerProperties() {
                return Collections.singletonList(new ListenerProperties(
                    "PLAINTEXT",
                    "PLAINTEXT://kafka:9092",
                    new Properties()
                ));
            }

            @Override
            public String getZookeeperConnectString() {
                return null;
            }
        });

        this.reuseFile = new ReuseFile.ReuseFileBuilder()
            .schemaRegistry("http://schema-registry:8085")
            .kafka("kafka:9092")
            .build();
    }

    public KafkaTestCluster(short numberOfBrokers, boolean reuse) {
        this.numberOfBrokers = numberOfBrokers;

        cluster = new com.salesforce.kafka.test.KafkaTestCluster(
            numberOfBrokers,
            new Properties() {{
                put("log.min.cleanable.dirty.ratio", "0.01");
                put("log.roll.ms", "1");
                put("log.cleaner.backoff.ms", "1");
                put("log.segment.delete.delay.ms", "1");
                put(TopicConfig.SEGMENT_MS_CONFIG, "1");
                put(TopicConfig.SEGMENT_INDEX_BYTES_CONFIG, "1");
                put(TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG, "0.01");
                put(TopicConfig.DELETE_RETENTION_MS_CONFIG, "1");
            }}
        );
        this.reuse = reuse;

        testUtils = new KafkaTestUtils(this.cluster);
    }

    @Override
    public void stop() {
        try {
            cluster.stop();
            schemaRegistry.stop();
            stream.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            cluster.start();

            logger.info("Kafka server started on {}", cluster.getKafkaConnectString());

            schemaRegistry = new RestApp(
                0,
                cluster.getZookeeperConnectString(),
                "__schemas",
                AvroCompatibilityLevel.BACKWARD.name,
                new Properties() {{
                    put(SchemaRegistryConfig.KAFKASTORE_TIMEOUT_CONFIG, "10000");
                    put(SchemaRegistryConfig.KAFKASTORE_INIT_TIMEOUT_CONFIG, "90000");
                }}
            );
            schemaRegistry.start();

            logger.info("Kafka Schema registry started on {}", schemaRegistry.restConnect);

            reuseFile = ReuseFile.builder()
                .kafka(cluster.getKafkaConnectString())
                .zookeeper(cluster.getZookeeperConnectString())
                .schemaRegistry(schemaRegistry.restConnect)
                .build();
            if (reuse) {
                writeClusterInfo();
            }

            injectTestData();
            logger.info("Test data injected");

            if (reuse) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        Files.delete(CS_PATH);
                    } catch (Exception e) {
                        logger.error("Can't delete CS file", e);
                    }
                }));
            }

        } catch (Exception  e) {
            throw new RuntimeException(e);
        }
    }

    public static ReuseFile readClusterInfo() throws IOException {
        if (!Files.exists(CS_PATH)) {
            return null;
        }

        return new Gson().fromJson(new String(Files.readAllBytes(CS_PATH)), ReuseFile.class);
    }

    public ReuseFile getClusterInfo() {
        return reuseFile;
    }

    private void writeClusterInfo() throws IOException {
        Files.write(
            CS_PATH,
            new GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(reuseFile)
                .getBytes()
        );
    }

    private void injectTestData() throws InterruptedException, ExecutionException {
        // empty topic
        testUtils.createTopic(TOPIC_EMPTY, 12, numberOfBrokers);
        logger.debug("Empty topic created");

        // random data
        testUtils.createTopic(TOPIC_RANDOM, 3, numberOfBrokers);
        for (int partition = 0; partition < 3; partition++) {
            testUtils.produceRecords(randomDatas(100, 0), TOPIC_RANDOM, partition);
        }
        logger.debug("Random topic created");

        // compacted topic
        testUtils.createTopic(TOPIC_COMPACTED, 3, numberOfBrokers);
        testUtils.getAdminClient().alterConfigs(ImmutableMap.of(
            new ConfigResource(ConfigResource.Type.TOPIC, TOPIC_COMPACTED),
            new Config(Collections.singletonList(
                new ConfigEntry(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT)
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
        logger.debug("Compacted topic created");

        // huge data
        testUtils.createTopic(TOPIC_HUGE, 3, numberOfBrokers);
        for (int partition = 0; partition < 3; partition++) {
            testUtils.produceRecords(randomDatas(1000, 0), TOPIC_HUGE, partition);
        }
        logger.debug("Huge topic created");

        // stream
        testUtils.createTopic(TOPIC_STREAM_IN, 3, numberOfBrokers);
        testUtils.createTopic(TOPIC_STREAM_MAP, 3, numberOfBrokers);
        testUtils.createTopic(TOPIC_STREAM_COUNT, 3, numberOfBrokers);
        stream = new StreamTest(this.reuseFile.getKafka(), this.reuseFile.getSchemaRegistry());
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
        logger.debug("Stream started");
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

    @Builder
    @Getter
    public static class ReuseFile {
        private String kafka;
        private String zookeeper;
        private String schemaRegistry;
    }
}