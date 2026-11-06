package org.akhq.clusters;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class SingleNodeKafkaCluster implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private static final String DEFAULT_KAFKA_IMAGE = "confluentinc/cp-kafka:8.1.0";
    private static final String DEFAULT_SCHEMA_REGISTRY_IMAGE = "confluentinc/cp-schema-registry:8.1.0";
    private static final String DEFAULT_KAFKA_CONNECT_IMAGE = "confluentinc/cp-kafka-connect:8.1.0";
    private static final String DEFAULT_KSQLDB_IMAGE = "confluentinc/cp-ksqldb-server:8.1.0";

    private ConfluentKafkaContainer kafka;
    private GenericContainer<?> schemaRegistry;
    private GenericContainer<?> kafkaConnect1;
    private GenericContainer<?> kafkaConnect2;
    private GenericContainer<?> ksqlDbServer;
    private String schemaRegistryUrl;
    private String kafkaConnect1Url;
    private String kafkaConnect2Url;
    private String ksqlDbServerUrl;
    private Network network;

    public SingleNodeKafkaCluster() {
    }

    public void start() {
        network = Network.newNetwork();

        log.debug("Starting embedded Kafka cluster using Testcontainers...");
        kafka = new ConfluentKafkaContainer(DockerImageName.parse(DEFAULT_KAFKA_IMAGE))
            .withNetworkAliases("kafka")
            .withNetwork(network)
            .withListener("kafka:29092")
            .withEnv("KAFKA_AUTHORIZER_CLASS_NAME", "org.apache.kafka.metadata.authorizer.StandardAuthorizer")
            .withEnv("KAFKA_ALLOW_EVERYONE_IF_NO_ACL_FOUND", "true")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
            .withStartupTimeout(java.time.Duration.ofMinutes(2))
            .withReuse(false);
        kafka.start();
        log.debug("Kafka broker started at {}", kafka.getBootstrapServers());

        schemaRegistry = new GenericContainer<>(DockerImageName.parse(DEFAULT_SCHEMA_REGISTRY_IMAGE))
            .withNetwork(network)
            .withNetworkAliases("registry")
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "localhost")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "kafka:29092")
            .withEnv("SCHEMA_REGISTRY_DEBUG", "true")
            .withExposedPorts(8081)
            .waitingFor(Wait.forHttp("/subjects"))
            .withStartupTimeout(java.time.Duration.ofMinutes(2))
            .withReuse(false);

        schemaRegistry.start();
        schemaRegistryUrl = String.format("http://localhost:%d", schemaRegistry.getMappedPort(8081));
        log.debug("Schema Registry started at {}", schemaRegistryUrl);

        kafkaConnect1 = new GenericContainer<>(DockerImageName.parse(DEFAULT_KAFKA_CONNECT_IMAGE))
            .withNetwork(network)
            .withNetworkAliases("connect1")
            .withEnv("CONNECT_BOOTSTRAP_SERVERS", "kafka:29092")
            .withEnv("CONNECT_REST_ADVERTISED_HOST_NAME", "connect1")
            .withEnv("CONNECT_GROUP_ID", "connect-cluster-1")
            .withEnv("CONNECT_CONFIG_STORAGE_TOPIC", "__connect-1-configs")
            .withEnv("CONNECT_OFFSET_STORAGE_TOPIC", "__connect-1-offsets")
            .withEnv("CONNECT_STATUS_STORAGE_TOPIC", "__connect-1-status")
            .withEnv("CONNECT_KEY_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
            .withEnv("CONNECT_VALUE_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
            .withEnv("CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("CONNECT_STATUS_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("CONNECT_SCHEMA_REGISTRY_URL", "http://registry:8081")
            .withEnv("CONNECT_PLUGIN_PATH", "/usr/local/share/kafka/plugins,/usr/share/filestream-connectors")
            .withExposedPorts(8083)
            .waitingFor(Wait.forHttp("/"))
            .withStartupTimeout(java.time.Duration.ofMinutes(2))
            .withReuse(false);

        kafkaConnect1.start();
        kafkaConnect1Url = String.format("http://localhost:%d", kafkaConnect1.getMappedPort(8083));
        log.debug("Kafka Connect 1 started at {}", kafkaConnect1Url);

        kafkaConnect2 = new GenericContainer<>(DockerImageName.parse(DEFAULT_KAFKA_CONNECT_IMAGE))
            .withNetwork(network)
            .withNetworkAliases("connect2")
            .withEnv("CONNECT_BOOTSTRAP_SERVERS", "kafka:29092")
            .withEnv("CONNECT_REST_ADVERTISED_HOST_NAME", "connect2")
            .withEnv("CONNECT_GROUP_ID", "connect-cluster-2")
            .withEnv("CONNECT_CONFIG_STORAGE_TOPIC", "__connect-2-configs")
            .withEnv("CONNECT_OFFSET_STORAGE_TOPIC", "__connect-2-offsets")
            .withEnv("CONNECT_STATUS_STORAGE_TOPIC", "__connect-2-status")
            .withEnv("CONNECT_KEY_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
            .withEnv("CONNECT_VALUE_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
            .withEnv("CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("CONNECT_STATUS_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("CONNECT_SCHEMA_REGISTRY_URL", "http://registry:8081")
            .withEnv("CONNECT_PLUGIN_PATH", "/usr/local/share/kafka/plugins,/usr/share/filestream-connectors")
            .withExposedPorts(8083)
            .waitingFor(Wait.forHttp("/"))
            .withStartupTimeout(java.time.Duration.ofMinutes(2))
            .withReuse(false);

        kafkaConnect2.start();
        kafkaConnect2Url = String.format("http://localhost:%d", kafkaConnect2.getMappedPort(8083));
        log.debug("Kafka Connect 2 started at {}", kafkaConnect2Url);

        ksqlDbServer = new GenericContainer<>(DockerImageName.parse(DEFAULT_KSQLDB_IMAGE))
            .withNetwork(network)
            .withNetworkAliases("ksqldb-server")
            .withEnv("KSQL_BOOTSTRAP_SERVERS", "kafka:29092")
            .withEnv("KSQL_HOST_NAME", "ksqldb-server")
            .withEnv("KSQL_LISTENERS", "http://ksqldb-server:8088")
            .withEnv("KSQL_KSQL_SCHEMA_REGISTRY_URL", "http://registry:8081")
            .withEnv("KSQL_KSQL_SERVICE_ID", "ksql")
            .withEnv("KSQL_KSQL_INTERNAL_TOPIC_REPLICAS", "1")
            .withEnv("KSQL_KSQL_STREAMS_REPLICATION_FACTOR", "1")
            .withEnv("KSQL_KSQL_SINK_REPLICAS", "1")
            .withEnv("KSQL_KSQL_LOGGING_PROCESSING_TOPIC_REPLICATION_FACTOR", "1")
            .withEnv("KSQL_KSQL_LOGGING_PROCESSING_STREAM_AUTO_CREATE", "true")
            .withEnv("KSQL_KSQL_LOGGING_PROCESSING_TOPIC_AUTO_CREATE", "true")
            .withEnv("KSQL_KSQL_STREAMS_NUM_STREAM_THREADS", "1")
            .withEnv("KSQL_KSQL_STREAMS_PROCESSING_GUARANTEE", "exactly_once_v2")
            .withEnv("KSQL_KSQL_STREAMS_PRODUCER_ACKS", "all")
            .withEnv("KSQL_PRODUCER_ENABLE_IDEMPOTENCE", "true")
            .withEnv("KSQL_PRODUCER_MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION", "5")
            .withEnv("KSQL_PRODUCER_ACKS", "all")
            //.withEnv("KSQL_PRODUCER_TRANSACTION_TIMEOUT_MS", "120000")
            //.withEnv("KSQL_PRODUCER_REQUEST_TIMEOUT_MS", "120000")
            //.withEnv("KSQL_PRODUCER_DELIVERY_TIMEOUT_MS", "120000")
            .withEnv("KSQL_KSQL_INTERNAL_TOPIC_MIN_INSYNC_REPLICAS", "1")
            .withExposedPorts(8088)
            .withStartupTimeout(java.time.Duration.ofMinutes(3))
            //.waitingFor(Wait.forHttp("/info"))
            .withReuse(false);

        ksqlDbServer.start();
        ksqlDbServerUrl = String.format("http://localhost:%d", ksqlDbServer.getMappedPort(8088));
        log.debug("ksqlDB Server started at {}", ksqlDbServerUrl);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        start();
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        stop();
    }

    public void stop() {
        log.info("Stopping EmbeddedSingleNodeKafkaCluster");
        if (ksqlDbServer != null) {
            ksqlDbServer.stop();
        }
        if (kafkaConnect1 != null) {
            kafkaConnect1.stop();
        }
        if (kafkaConnect2 != null) {
            kafkaConnect2.stop();
        }
        if (schemaRegistry != null) {
            schemaRegistry.stop();
        }
        if (kafka != null) {
            kafka.stop();
        }
        log.info("EmbeddedSingleNodeKafkaCluster Stopped");
    }

    public String bootstrapServers() {
        return kafka.getBootstrapServers();
    }

    public String schemaRegistryUrl() {
        return schemaRegistryUrl;
    }

    public String kafkaConnect1Url() {
        return kafkaConnect1Url;
    }

    public String kafkaConnect2Url() {
        return kafkaConnect2Url;
    }

    public String ksqlDbServerUrl() {
        return ksqlDbServerUrl;
    }
}
