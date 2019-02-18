package org.kafkahq.clusters;

import io.confluent.kafka.schemaregistry.RestApp;
import io.confluent.kafka.schemaregistry.avro.AvroCompatibilityLevel;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig;
import kafka.server.KafkaConfig$;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.util.Properties;

/**
 * Runs an in-memory, "embedded" Kafka cluster with 1 ZooKeeper instance, 1 Kafka broker, and 1
 * Confluent Schema Registry instance.
 */
@Slf4j
public class EmbeddedSingleNodeKafkaCluster implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private static final int DEFAULT_BROKER_PORT = 0; // 0 results in a random port being selected
    private static final String KAFKA_SCHEMAS_TOPIC = "__schemas";
    private static final String AVRO_COMPATIBILITY_TYPE = AvroCompatibilityLevel.BACKWARD.name;

    private static final String KAFKASTORE_OPERATION_TIMEOUT_MS = "10000";
    private static final String KAFKASTORE_DEBUG = "true";
    private static final String KAFKASTORE_INIT_TIMEOUT = "90000";

    private ZooKeeperEmbedded zookeeper;
    private KafkaEmbedded broker;
    private RestApp schemaRegistry;
    private final Properties brokerConfig;
    private boolean running;

    /**
     * Creates and starts the cluster.
     *
     * @param brokerConfig Additional broker configuration settings.
     */
    public EmbeddedSingleNodeKafkaCluster(final Properties brokerConfig) {
        this.brokerConfig = new Properties();
        this.brokerConfig.putAll(brokerConfig);
    }

    /**
     * Creates and starts the cluster.
     */
    public void start() throws Exception {
        log.debug("Initiating embedded Kafka cluster startup");
        log.debug("Starting a ZooKeeper instance...");
        zookeeper = new ZooKeeperEmbedded();
        log.debug("ZooKeeper instance is running at {}", zookeeper.connectString());

        final Properties effectiveBrokerConfig = effectiveBrokerConfigFrom(brokerConfig, zookeeper);
        log.debug("Starting a Kafka instance on port {} ...",
            effectiveBrokerConfig.getProperty(KafkaConfig$.MODULE$.PortProp()));
        broker = new KafkaEmbedded(effectiveBrokerConfig);
        log.debug("Kafka instance is running at {}, connected to ZooKeeper at {}", broker.brokerList(), broker.zookeeperConnect());

        final Properties schemaRegistryProps = new Properties();
        schemaRegistryProps.put(SchemaRegistryConfig.KAFKASTORE_TIMEOUT_CONFIG, KAFKASTORE_OPERATION_TIMEOUT_MS);
        schemaRegistryProps.put(SchemaRegistryConfig.DEBUG_CONFIG, KAFKASTORE_DEBUG);
        schemaRegistryProps.put(SchemaRegistryConfig.KAFKASTORE_INIT_TIMEOUT_CONFIG, KAFKASTORE_INIT_TIMEOUT);

        schemaRegistry = new RestApp(0, zookeeperConnect(), KAFKA_SCHEMAS_TOPIC, AVRO_COMPATIBILITY_TYPE, schemaRegistryProps);
        schemaRegistry.start();
        running = true;
    }

    private Properties effectiveBrokerConfigFrom(final Properties brokerConfig, final ZooKeeperEmbedded zookeeper) {
        final Properties effectiveConfig = new Properties();
        effectiveConfig.putAll(brokerConfig);
        effectiveConfig.put(KafkaConfig$.MODULE$.ZkConnectProp(), zookeeper.connectString());
        effectiveConfig.put(KafkaConfig$.MODULE$.ZkSessionTimeoutMsProp(), 30 * 1000);
        effectiveConfig.put(KafkaConfig$.MODULE$.PortProp(), DEFAULT_BROKER_PORT);
        effectiveConfig.put(KafkaConfig$.MODULE$.ZkConnectionTimeoutMsProp(), 60 * 1000);
        effectiveConfig.put(KafkaConfig$.MODULE$.DeleteTopicEnableProp(), true);
        effectiveConfig.put(KafkaConfig$.MODULE$.LogCleanerDedupeBufferSizeProp(), 2 * 1024 * 1024L);
        effectiveConfig.put(KafkaConfig$.MODULE$.GroupMinSessionTimeoutMsProp(), 0);
        effectiveConfig.put(KafkaConfig$.MODULE$.OffsetsTopicReplicationFactorProp(), (short) 1);
        effectiveConfig.put(KafkaConfig$.MODULE$.OffsetsTopicPartitionsProp(), 1);
        effectiveConfig.put(KafkaConfig$.MODULE$.AutoCreateTopicsEnableProp(), true);
        return effectiveConfig;
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        start();
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        stop();
    }

    /**
     * Stops the cluster.
     */
    public void stop() {
        log.info("Stopping Confluent");
        try {
            try {
                if (schemaRegistry != null) {
                    schemaRegistry.stop();
                }
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
            if (broker != null) {
                broker.stop();
            }
            try {
                if (zookeeper != null) {
                    zookeeper.stop();
                }
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            running = false;
        }
        log.info("Confluent Stopped");
    }

    /**
     * This cluster's `bootstrap.servers` value.  Example: `127.0.0.1:9092`.
     *
     * You can use this to tell Kafka Streams applications, Kafka producers, and Kafka consumers (new
     * consumer API) how to connect to this cluster.
     */
    public String bootstrapServers() {
        return broker.brokerList();
    }

    /**
     * This cluster's ZK connection string aka `zookeeper.connect` in `hostnameOrIp:port` format.
     * Example: `127.0.0.1:2181`.
     *
     * You can use this to e.g. tell Kafka consumers (old consumer API) how to connect to this
     * cluster.
     */
    public String zookeeperConnect() {
        return zookeeper.connectString();
    }

    /**
     * The "schema.registry.url" setting of the schema registry instance.
     */
    public String schemaRegistryUrl() {
        return schemaRegistry.restConnect;
    }
}