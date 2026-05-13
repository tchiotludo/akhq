package org.akhq.clients.connect;

import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.clients.connect.dto.ConnectorExpanded;
import org.akhq.clients.connect.dto.ConnectorPluginValidation;
import org.akhq.clients.connect.error.ConnectBadRequestException;
import org.akhq.clients.connect.error.ConnectNotFoundException;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for KafkaConnectApiClient focused on behaviour not covered by ConnectRepositoryTest:
 * - HTTP error mapping (404, 400)
 * - getConnectorsExpanded (expand=info&status endpoint)
 * - validateConnectorPluginConfig
 */
class KafkaConnectApiClientIntegrationTest extends AbstractTest {

    static final String FILE_PATH = Objects.requireNonNull(
        KafkaConnectApiClientIntegrationTest.class.getClassLoader().getResource("application.yml")
    ).getPath();

    static KafkaConnectApiClient client;

    @BeforeAll
    static void setup() throws IOException {
        KafkaTestCluster.ConnectionString cs = KafkaTestCluster.readClusterInfo();
        client = KafkaConnectApiClient.builder(cs.getConnect1()).build();
    }

    @Test
    void getUnknownConnectorThrowsNotFoundException() {
        assertThrows(ConnectNotFoundException.class, () -> client.getConnector("does-not-exist"));
    }

    @Test
    void createWithInvalidClassThrowsBadRequestException() {
        assertThrows(ConnectBadRequestException.class, () ->
            client.createConnector("bad-connector", Map.of(
                "connector.class", "NonExistentConnector",
                "topics", KafkaTestCluster.TOPIC_CONNECT
            ))
        );
    }

    @Test
    void getConnectorsExpandedContainsInfoAndStatus() {
        // Create a connector so the expanded list is non-empty
        String name = "client-test-expanded";
        client.createConnector(name, Map.of(
            "connector.class", "FileStreamSinkConnector",
            "tasks.max", "1",
            "topics", KafkaTestCluster.TOPIC_CONNECT,
            "file", FILE_PATH
        ));
        try {
            Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> client.getConnectorsExpanded().containsKey(name));

            Map<String, ConnectorExpanded> expanded = client.getConnectorsExpanded();
            assertTrue(expanded.containsKey(name));
            assertNotNull(expanded.get(name).getInfo());
            assertNotNull(expanded.get(name).getInfo().getConfig());
            assertNotNull(expanded.get(name).getStatus());
            assertNotNull(expanded.get(name).getStatus().getConnector().getState());
        } finally {
            client.deleteConnector(name);
        }
    }

    @Test
    void validatePluginConfigReturnsDefinitions() {
        ConnectorPluginValidation result = client.validateConnectorPluginConfig(
            "FileStreamSinkConnector",
            Map.of(
                "connector.class", "org.apache.kafka.connect.file.FileStreamSinkConnector",
                "topics", KafkaTestCluster.TOPIC_CONNECT
            )
        );
        assertNotNull(result);
        assertFalse(result.getConfigs().isEmpty());
    }
}

