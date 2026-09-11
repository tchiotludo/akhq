package org.akhq.controllers;

import io.micronaut.http.HttpRequest;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AkhqToolsTest extends AbstractTest {
    private static final String URL = "/mcp";

    @Test
    void initialize() {
        Map<String, Object> payload = Map.of(
            "jsonrpc", "2.0",
            "id", "init-1",
            "method", "initialize",
            "params", Map.of(
                "protocolVersion", "2025-11-25",
                "capabilities", Map.of(),
                "clientInfo", Map.of("name", "akhq-test", "version", "0.1.0")
            )
        );

        Map<String, Object> response = this.retrieve(HttpRequest.POST(URL, payload), Map.class);
        assertNotNull(response.get("result"), String.valueOf(response));
        Map<String, Object> result = map(response.get("result"));

        assertEquals("2.0", response.get("jsonrpc"));
        assertEquals("init-1", response.get("id"));
        assertEquals("2025-11-25", result.get("protocolVersion"));
        assertNotNull(result.get("capabilities"), String.valueOf(result));
    }

    @Test
    void toolsList() {
        Map<String, Object> payload = Map.of(
            "jsonrpc", "2.0",
            "id", "list-1",
            "method", "tools/list",
            "params", Map.of()
        );

        Map<String, Object> response = this.retrieve(HttpRequest.POST(URL, payload), Map.class);
        assertNotNull(response.get("result"), String.valueOf(response));
        Map<String, Object> result = map(response.get("result"));

        assertEquals("2.0", response.get("jsonrpc"));
        java.util.List<String> names = list(result.get("tools")).stream()
            .map(this::map)
            .map(tool -> String.valueOf(tool.get("name")))
            .toList();

        assertTrue(names.contains("akhq.find_message_in_topic"));
        assertTrue(names.contains("akhq.get_message_detail"));
    }

    @Test
    void toolsCallFound() {
        Map<String, Object> payload = Map.of(
            "jsonrpc", "2.0",
            "id", "call-1",
            "method", "tools/call",
            "params", Map.of(
                "name", "akhq.find_message_in_topic",
                "arguments", Map.of(
                    "arguments", Map.of(
                        "cluster", KafkaTestCluster.CLUSTER_ID,
                        "topic", KafkaTestCluster.TOPIC_RANDOM,
                        "searchByValue", "42_C",
                        "maxMatches", 1
                    )
                )
            )
        );

        Map<String, Object> response = this.retrieve(HttpRequest.POST(URL, payload), Map.class);
        assertNotNull(response.get("result"), String.valueOf(response));
        Map<String, Object> result = map(response.get("result"));

        assertEquals("2.0", response.get("jsonrpc"));
        Map<String, Object> structured = tryGetStructuredContent(result);
        if (structured != null) {
            assertTrue((Boolean) structured.get("found"));
            assertEquals(KafkaTestCluster.TOPIC_RANDOM, structured.get("topic"));
            assertEquals(1, structured.get("matchCount"));

            java.util.List<Object> messages = list(structured.get("messages"));
            assertFalse(messages.isEmpty());
            Map<String, Object> firstMessage = map(messages.getFirst());
            assertNotNull(firstMessage.get("partition"));
            assertNotNull(firstMessage.get("offset"));
            assertNotNull(firstMessage.get("timestamp"));
            assertTrue(firstMessage.containsKey("key"));
            assertNotNull(firstMessage.get("valueOverview"));
        } else {
            assertTrue(flattenContent(result).contains("Found"), String.valueOf(result));
        }
    }

    @Test
    void toolsCallNotFound() {
        Map<String, Object> payload = Map.of(
            "jsonrpc", "2.0",
            "id", "call-2",
            "method", "tools/call",
            "params", Map.of(
                "name", "akhq.find_message_in_topic",
                "arguments", Map.of(
                    "arguments", Map.of(
                        "cluster", KafkaTestCluster.CLUSTER_ID,
                        "topic", KafkaTestCluster.TOPIC_RANDOM,
                        "searchByValue", "value-does-not-exist_C",
                        "maxMatches", 1
                    )
                )
            )
        );

        Map<String, Object> response = this.retrieve(HttpRequest.POST(URL, payload), Map.class);
        assertNotNull(response.get("result"), String.valueOf(response));
        Map<String, Object> result = map(response.get("result"));

        assertEquals("2.0", response.get("jsonrpc"));
        Map<String, Object> structured = tryGetStructuredContent(result);
        if (structured != null) {
            assertFalse((Boolean) structured.get("found"));
            assertEquals(0, structured.get("matchCount"));
            assertTrue(list(structured.get("messages")).isEmpty());
            assertNotNull(structured.get("timeWindowSuggestion"));

            Map<String, Object> suggestion = map(structured.get("timeWindowSuggestion"));
            assertNotNull(suggestion.get("timestamp"));
            assertNotNull(suggestion.get("endTimestamp"));
        } else {
            assertTrue(flattenContent(result).contains("No matching message"), String.valueOf(result));
        }
    }

    @Test
    void toolsCallMessageDetailFound() {
        Map<String, Object> payload = Map.of(
            "jsonrpc", "2.0",
            "id", "call-3",
            "method", "tools/call",
            "params", Map.of(
                "name", "akhq.get_message_detail",
                "arguments", Map.of(
                    "arguments", Map.of(
                        "cluster", KafkaTestCluster.CLUSTER_ID,
                        "topic", KafkaTestCluster.TOPIC_RANDOM,
                        "partition", 0,
                        "offset", 0
                    )
                )
            )
        );

        Map<String, Object> response = this.retrieve(HttpRequest.POST(URL, payload), Map.class);
        assertNotNull(response.get("result"), String.valueOf(response));
        Map<String, Object> result = map(response.get("result"));

        assertEquals("2.0", response.get("jsonrpc"));
        Map<String, Object> structured = tryGetStructuredContent(result);
        if (structured != null) {
            assertTrue((Boolean) structured.get("found"));
            assertEquals(KafkaTestCluster.TOPIC_RANDOM, structured.get("topic"));
            assertEquals(0, structured.get("partition"));
            assertEquals(0, ((Number) structured.get("offset")).longValue());
            assertNotNull(structured.get("value"));
        } else {
            String content = flattenContent(result);
            assertTrue(content.contains("\"found\":true"), content);
            assertTrue(content.contains("\"topic\":\"" + KafkaTestCluster.TOPIC_RANDOM + "\""), content);
            assertTrue(content.contains("\"partition\":0"), content);
            assertTrue(content.contains("\"offset\":0"), content);
            assertTrue(content.contains("\"value\":"), content);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private java.util.List<Object> list(Object value) {
        return (java.util.List<Object>) value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> tryGetStructuredContent(Map<String, Object> result) {
        Object structured = result.get("structuredContent");
        if (structured instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private String flattenContent(Map<String, Object> result) {
        Object content = result.get("content");
        if (!(content instanceof java.util.List<?> list) || list.isEmpty()) {
            return String.valueOf(result);
        }
        Object first = list.getFirst();
        if (first instanceof Map<?, ?> contentPart) {
            Object text = contentPart.containsKey("text") ? contentPart.get("text") : result;
            return String.valueOf(text);
        }
        return String.valueOf(first);
    }
}
