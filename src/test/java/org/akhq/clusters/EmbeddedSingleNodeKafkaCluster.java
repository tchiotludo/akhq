package org.akhq.clusters;

import io.confluent.kafka.schemaregistry.CompatibilityLevel;
import io.confluent.kafka.schemaregistry.RestApp;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig;
import kafka.server.KafkaConfig$;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.util.Properties;

@Slf4j
public class EmbeddedSingleNodeKafkaCluster implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private static final int DEFAULT_BROKER_PORT = 0; // 0 results in a random port being selected
    private static final String KAFKA_SCHEMAS_TOPIC = "__schemas";
    private static final String AVRO_COMPATIBILITY_TYPE = CompatibilityLevel.BACKWARD.name;

    private static final String KAFKASTORE_OPERATION_TIMEOUT_MS = "10000";
    private static final String KAFKASTORE_DEBUG = "true";
    private static final String KAFKASTORE_INIT_TIMEOUT = "90000";

    private ZooKeeperEmbedded zookeeper;
    private KafkaEmbedded broker;
    private RestApp schemaRegistry;
    private ConnectEmbedded connect1, connect2;
    private final Properties brokerConfig;

    public EmbeddedSingleNodeKafkaCluster(final Properties brokerConfig) {
        this.brokerConfig = new Properties();
        this.brokerConfig.putAll(brokerConfig);
    }

    public void start() throws Exception {
        // zookeeper
        log.debug("Initiating embedded Kafka cluster startup");
        log.debug("Starting a ZooKeeper instance...");
        zookeeper = new ZooKeeperEmbedded();
        log.debug("ZooKeeper instance is running at {}", zookeeper.connectString());

        // kafka
        final Properties effectiveBrokerConfig = effectiveBrokerConfigFrom(brokerConfig, zookeeper);
        log.debug("Starting a Kafka instance on port {} ...", effectiveBrokerConfig.getProperty(KafkaConfig$.MODULE$.PortProp()));
        broker = new KafkaEmbedded(effectiveBrokerConfig);
        log.debug("Kafka instance is running at {}, connected to ZooKeeper at {}", broker.brokerList(), broker.zookeeperConnect());

        // schema registry
        final Properties schemaRegistryProps = new Properties();
        schemaRegistryProps.put(SchemaRegistryConfig.KAFKASTORE_TIMEOUT_CONFIG, KAFKASTORE_OPERATION_TIMEOUT_MS);
        schemaRegistryProps.put(SchemaRegistryConfig.DEBUG_CONFIG, KAFKASTORE_DEBUG);
        schemaRegistryProps.put(SchemaRegistryConfig.KAFKASTORE_INIT_TIMEOUT_CONFIG, KAFKASTORE_INIT_TIMEOUT);

        schemaRegistry = new RestApp(0, zookeeperConnect(), KAFKA_SCHEMAS_TOPIC, AVRO_COMPATIBILITY_TYPE, schemaRegistryProps);
        schemaRegistry.start();
        log.debug("Schema registry is running at {}", schemaRegistryUrl());

        // connect-1
        Properties connect1Properties = new Properties();
        connect1Properties.put("bootstrap.servers", bootstrapServers());
        connect1Properties.put("key.converter", "io.confluent.connect.avro.AvroConverter");
        connect1Properties.put("key.converter.schema.registry.url", schemaRegistryUrl());
        connect1Properties.put("value.converter", "io.confluent.connect.avro.AvroConverter");
        connect1Properties.put("value.converter.schema.registry.url", schemaRegistryUrl());
        connect1Properties.put("rest.port", "0");
        connect1Properties.put("group.id", "connect-1-integration-test-");
        connect1Properties.put("offset.storage.topic", "__connect-1-offsets");
        connect1Properties.put("offset.storage.replication.factor", 1);
        connect1Properties.put("config.storage.topic", "__connect-1-config");
        connect1Properties.put("config.storage.replication.factor", 1);
        connect1Properties.put("status.storage.topic", "__connect-1-status");
        connect1Properties.put("status.storage.replication.factor", 1);
        connect1Properties.put("plugin.path", "null");

        // connect-2
        Properties connect2Properties = new Properties();
        connect2Properties.put("bootstrap.servers", bootstrapServers());
        connect2Properties.put("key.converter", "io.confluent.connect.avro.AvroConverter");
        connect2Properties.put("key.converter.schema.registry.url", schemaRegistryUrl());
        connect2Properties.put("value.converter", "io.confluent.connect.avro.AvroConverter");
        connect2Properties.put("value.converter.schema.registry.url", schemaRegistryUrl());
        connect2Properties.put("rest.port", "0");
        connect2Properties.put("group.id", "connect-2-integration-test-");
        connect2Properties.put("offset.storage.topic", "__connect-2-offsets");
        connect2Properties.put("offset.storage.replication.factor", 1);
        connect2Properties.put("config.storage.topic", "__connect-2-config");
        connect2Properties.put("config.storage.replication.factor", 1);
        connect2Properties.put("status.storage.topic", "__connect-2-status");
        connect2Properties.put("status.storage.replication.factor", 1);
        connect2Properties.put("plugin.path", "null");

        connect1 = new ConnectEmbedded(connect1Properties);
        log.debug("Kafka Connect-1 is running at {}", connect1Url());

        connect2 = new ConnectEmbedded(connect2Properties);
        log.debug("Kafka Connect-2 is running at {}", connect2Url());
    }

    private Properties effectiveBrokerConfigFrom(final Properties brokerConfig, final ZooKeeperEmbedded zookeeper) {
        final Properties effectiveConfig = new Properties();
        effectiveConfig.putAll(brokerConfig);
        effectiveConfig.put(KafkaConfig$.MODULE$.ZkConnectProp(), zookeeper.connectString());
        effectiveConfig.put(KafkaConfig$.MODULE$.PortProp(), DEFAULT_BROKER_PORT);
        effectiveConfig.put(KafkaConfig$.MODULE$.DeleteTopicEnableProp(), true);
        effectiveConfig.put(KafkaConfig$.MODULE$.LogCleanerDedupeBufferSizeProp(), 2 * 1024 * 1024L);
        effectiveConfig.put(KafkaConfig$.MODULE$.GroupMinSessionTimeoutMsProp(), 0);
        effectiveConfig.put(KafkaConfig$.MODULE$.GroupInitialRebalanceDelayMsProp(), 0);
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

    public void stop() {
        log.info("Stopping EmbeddedSingleNodeKafkaCluster");
        try {
            if (connect1 != null) {
                connect1.stop();
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        try {
            if (connect2 != null) {
                connect2.stop();
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

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
        log.info("EmbeddedSingleNodeKafkaCluster Stopped");
    }

    public String bootstrapServers() {
        return broker.brokerList();
    }

    public String zookeeperConnect() {
        return zookeeper.connectString();
    }

    public String schemaRegistryUrl() {
        return schemaRegistry.restConnect;
    }

    public String connect1Url() {
        return connect1.connectUrl();
    }

    public String connect2Url() {
        return connect2.connectUrl();
    }
}
