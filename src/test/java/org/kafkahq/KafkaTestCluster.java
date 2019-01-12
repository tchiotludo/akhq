package org.kafkahq;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.salesforce.kafka.test.KafkaTestUtils;
import com.yammer.metrics.core.Stoppable;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class KafkaTestCluster implements Runnable, Stoppable {
    public static final Path CS_PATH = Paths.get(System.getProperty("java.io.tmpdir"), "/kafkahq-cs.txt");

    public static final String CLUSTER_ID = "test";

    public static final String TOPIC_RANDOM = "random";
    public static final String TOPIC_COMPACTED = "compacted";
    public static final String TOPIC_EMPTY = "empty";

    private final static Logger logger = LoggerFactory.getLogger(RecordRepository.class);
    private final short numberOfBrokers;
    private final boolean reuse;
    private final com.salesforce.kafka.test.KafkaTestCluster cluster;
    private final KafkaTestUtils testUtils;

    public com.salesforce.kafka.test.KafkaTestCluster getCluster() {
        return cluster;
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            cluster.start();

            if (reuse) {
                writeClusterInfo();
            }

            logger.info("Kafka server started on {}", cluster.getKafkaConnectString());

            injectTestData();
            logger.info("Test data injected");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.delete(CS_PATH);
                } catch (Exception e) {
                    logger.error("Can't delete CS file", e);
                }
            }));

        } catch (Exception  e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        KafkaTestCluster kafkaTestCluster = new KafkaTestCluster((short) 3, true);
        kafkaTestCluster.run();
    }

    private void writeClusterInfo() throws IOException {
        Files.write(
            CS_PATH,
            cluster.getKafkaConnectString().getBytes()
        );
    }

    private void injectTestData() throws InterruptedException, ExecutionException {
        // empty topic
        testUtils.createTopic(TOPIC_EMPTY, 12, numberOfBrokers);

        // random data
        testUtils.createTopic(TOPIC_RANDOM, 3, numberOfBrokers);
        for (int partition = 0; partition < 3; partition++) {
            testUtils.produceRecords(randomDatas(100, 0), TOPIC_RANDOM, partition);
        }

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

        Thread.sleep(100L);
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
}