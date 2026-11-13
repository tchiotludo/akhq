package org.akhq.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in test against tests/kraft/docker-compose.yml; never connects to a cluster by default. */
@EnabledIfEnvironmentVariable(named = "AKHQ_KRAFT_BOOTSTRAP", matches = ".+")
class MetadataQuorumIntegrationTest {
    @Test
    void httpApiShowsDedicatedControllersAndMatchesKafkaLeader() throws Exception {
        String bootstrap = System.getenv("AKHQ_KRAFT_BOOTSTRAP");
        Map<String, Object> config = Map.of(
            "micronaut.server.port", -1,
            "micronaut.server.host", "127.0.0.1",
            "micronaut.security.enabled", false,
            "endpoints.all.enabled", false,
            "akhq.connections.kraft.properties.bootstrap.servers", bootstrap
        );
        try (EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class, config);
             AdminClient admin = AdminClient.create(Map.of("bootstrap.servers", bootstrap))) {
            var actual = admin.describeMetadataQuorum().quorumInfo().get(15, TimeUnit.SECONDS);
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            var response = client.send(HttpRequest.newBuilder(
                URI.create(server.getURI() + "/api/kraft/node/quorum")).timeout(Duration.ofSeconds(15)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());
            var json = new ObjectMapper().readTree(response.body());
            assertEquals("AVAILABLE", json.path("status").asText(), response.body());
            assertEquals(actual.leaderId(), json.path("leaderId").asInt());
            Set<Integer> ids = new HashSet<>();
            int leaders = 0;
            for (var voter : json.path("voters")) {
                ids.add(voter.path("id").asInt());
                if (voter.path("leader").asBoolean()) leaders++;
            }
            assertEquals(Set.of(100, 101, 102), ids);
            assertEquals(1, leaders);

            var brokersResponse = client.send(HttpRequest.newBuilder(
                URI.create(server.getURI() + "/api/kraft/node")).timeout(Duration.ofSeconds(15)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, brokersResponse.statusCode(), brokersResponse.body());
            var brokers = new ObjectMapper().readTree(brokersResponse.body()).path("nodes");
            assertEquals(3, brokers.size());
            for (var broker : brokers) assertFalse(ids.contains(broker.path("id").asInt()));
            System.out.println("Verified Kafka quorum API: " + response.body());
        }
    }
}
