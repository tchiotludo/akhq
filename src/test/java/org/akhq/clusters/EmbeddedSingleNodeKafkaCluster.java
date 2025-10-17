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
public class EmbeddedSingleNodeKafkaCluster implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private static final String DEFAULT_KAFKA_IMAGE = "confluentinc/cp-kafka:8.0.0";
    private static final String DEFAULT_SCHEMA_REGISTRY_IMAGE = "confluentinc/cp-schema-registry:8.0.0";
    private static final String DEFAULT_KAFKA_CONNECT_IMAGE = "confluentinc/cp-kafka-connect:8.0.0";
    private static final String DEFAULT_KSQLDB_IMAGE = "confluentinc/ksqldb-server:0.29.0";

    private ConfluentKafkaContainer kafka;
    private GenericContainer<?> schemaRegistry;
    private GenericContainer<?> kafkaConnect;
    private GenericContainer<?> ksqlDbServer;
    private String schemaRegistryUrl;
    private String kafkaConnectUrl;
    private String ksqlDbServerUrl;
    private Network network;

    public EmbeddedSingleNodeKafkaCluster() {
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
            .withReuse(false);

        schemaRegistry.start();
        schemaRegistryUrl = String.format("http://localhost:%d", schemaRegistry.getMappedPort(8081));
        log.debug("Schema Registry started at {}", schemaRegistryUrl);

        kafkaConnect = new GenericContainer<>(DockerImageName.parse(DEFAULT_KAFKA_CONNECT_IMAGE))
            .withNetwork(network)
            .withNetworkAliases("connect")
            .withEnv("CONNECT_BOOTSTRAP_SERVERS", "kafka:29092")
            .withEnv("CONNECT_REST_ADVERTISED_HOST_NAME", "connect")
            .withEnv("CONNECT_GROUP_ID", "connect-cluster")
            .withEnv("CONNECT_CONFIG_STORAGE_TOPIC", "connect-configs")
            .withEnv("CONNECT_OFFSET_STORAGE_TOPIC", "connect-offsets")
            .withEnv("CONNECT_STATUS_STORAGE_TOPIC", "connect-status")
            .withEnv("CONNECT_KEY_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
            .withEnv("CONNECT_VALUE_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
            .withEnv("CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("CONNECT_STATUS_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("CONNECT_SCHEMA_REGISTRY_URL", "http://registry:8081")
            .withEnv("CONNECT_PLUGIN_PATH", "/usr/local/share/kafka/plugins,/usr/share/filestream-connectors")
            .withExposedPorts(8083)
            .waitingFor(Wait.forHttp("/"))
            .withReuse(false);

        kafkaConnect.start();
        kafkaConnectUrl = String.format("http://localhost:%d", kafkaConnect.getMappedPort(8083));
        log.debug("Kafka Connect started at {}", kafkaConnectUrl);

        ksqlDbServer = new GenericContainer<>(DockerImageName.parse(DEFAULT_KSQLDB_IMAGE))
            .withNetwork(network)
            .withNetworkAliases("ksqldb-server")
            .withEnv("KSQL_BOOTSTRAP_SERVERS", "kafka:29092")
            .withEnv("KSQL_HOST_NAME", "ksqldb-server")
            .withEnv("KSQL_LISTENERS", "http://0.0.0.0:8088")
            .withEnv("KSQL_KSQL_SCHEMA_REGISTRY_URL", "http://registry:8081")
            .withExposedPorts(8088)
            .waitingFor(Wait.forHttp("/info"))
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
        if (kafkaConnect != null) {
            kafkaConnect.stop();
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

    public String kafkaConnectUrl() {
        return kafkaConnectUrl;
    }

    public String ksqlDbServerUrl() {
        return ksqlDbServerUrl;
    }
}
