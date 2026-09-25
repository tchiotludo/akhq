package org.akhq.controllers;

import com.google.common.collect.ImmutableMap;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.sse.SseClient;
import io.micronaut.runtime.server.EmbeddedServer;
import jakarta.inject.Inject;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class TopicDataMessageLengthControllerTest extends AbstractTest {
    private static final String INTERLEAVED_URL = "/api/" + KafkaTestCluster.CLUSTER_ID + "/topic/" + KafkaTestCluster.TOPIC_INTERLEAVED;
    // TOPIC_INTERLEAVED values ("value_1", "value_10", ...) are all longer than this limit.
    private static final String FILTER = "searchByKey=key_1_E";

    @Inject
    private EmbeddedServer embeddedServer;

    @NonNull
    @Override
    public Map<String, String> getProperties() {
        return ImmutableMap.<String, String>builder()
            .putAll(super.getProperties())
            .put("akhq.topic-data.kafka-max-message-length", "6")
            .build();
    }

    @Test
    void searchTruncatesLargeValues() {
        SseClient sseClient = embeddedServer.getApplicationContext().createBean(SseClient.class, embeddedServer.getURL());
        HttpRequest<?> request = HttpRequest.GET(URI.create(INTERLEAVED_URL + "/data/search?" + FILTER))
            .basicAuth("admin", "pass");

        List<Map<?, ?>> records = Flux.from(sseClient.eventStream(request, String.class))
            .collectList()
            .block()
            .stream()
            .map(event -> new JSONObject(event.getData()))
            .filter(data -> data.has("records"))
            .flatMap(data -> data.getJSONArray("records").toList().stream())
            .<Map<?, ?>>map(record -> (Map<?, ?>) record)
            .toList();

        assertEquals(1, records.size());
        // The limit keeps 6 / 1000 = 0 chars, and empty strings are omitted from the JSON.
        assertNull(records.get(0).get("value"));
        assertEquals(Boolean.TRUE, records.get(0).get("truncated"));
    }

    @Test
    void downloadKeepsFullValues() {
        String body = client.toBlocking().retrieve(
            HttpRequest.GET(INTERLEAVED_URL + "/data/download?" + FILTER).basicAuth("admin", "pass")
        );

        JSONArray records = new JSONArray(body);
        List<String> values = new ArrayList<>();
        for (int i = 0; i < records.length(); i++) {
            values.add(records.getJSONObject(i).getString("value"));
            assertFalse(records.getJSONObject(i).has("truncated"));
        }

        assertEquals(List.of("value_1"), values);
    }
}
