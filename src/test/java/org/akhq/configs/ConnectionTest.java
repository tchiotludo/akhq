package org.akhq.configs;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionTest {
    @Test
    void topicDiscoveryAllowlistBindsPerConnection() {
        try (ApplicationContext applicationContext = ApplicationContext.run(Map.of(
            "akhq.connections.cluster-a.properties.bootstrap.servers", "localhost:9092",
            "akhq.connections.cluster-a.topic-discovery.allowlist[0]", "topic-a",
            "akhq.connections.cluster-a.topic-discovery.allowlist[1]", "topic-b",
            "akhq.connections.cluster-b.properties.bootstrap.servers", "localhost:9093"
        ))) {
            Collection<Connection> connections = applicationContext.getBeansOfType(Connection.class);
            Connection clusterA = connection(connections, "cluster-a");
            Connection clusterB = connection(connections, "cluster-b");

            assertEquals(List.of("topic-a", "topic-b"), clusterA.getTopicDiscovery().getAllowlist());
            assertTrue(clusterB.getTopicDiscovery().getAllowlist().isEmpty());
        }
    }

    private Connection connection(Collection<Connection> connections, String name) {
        return connections.stream()
            .filter(connection -> name.equals(connection.getName()))
            .findFirst()
            .orElseThrow();
    }
}
