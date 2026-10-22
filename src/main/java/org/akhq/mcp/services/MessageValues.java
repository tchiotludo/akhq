package org.akhq.mcp.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Shapes message values for MCP tool results, which end up in the context of an LLM.
 */
final class MessageValues {
    static final int MIN_VALUE_LENGTH = 200;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MessageValues() {
    }

    /**
     * Extracts the given paths of a JSON value.
     *
     * @param value the message value
     * @param paths dot separated paths, where a numeric segment indexes an array, such as {@code items.0.price}
     * @return the value of every path, {@code null} for a missing path, or empty when the value is not a JSON
     * object or array
     */
    static Optional<Map<String, Object>> project(String value, List<String> paths) {
        if (value == null) {
            return Optional.empty();
        }

        JsonNode root;
        try {
            root = MAPPER.readTree(value);
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
        if (root == null || !root.isContainerNode()) {
            return Optional.empty();
        }

        Map<String, Object> projection = new LinkedHashMap<>();
        for (String path : paths) {
            projection.put(path, toValue(resolve(root, path)));
        }
        return Optional.of(projection);
    }

    /**
     * Largest value length such that every value, truncated to it, fits the budget. Short values stay complete and
     * the budget left is shared evenly by the longer ones.
     *
     * @return {@link Integer#MAX_VALUE} when every value fits, and never less than {@link #MIN_VALUE_LENGTH}
     */
    static int maxValueLength(List<Integer> lengths, int budget) {
        List<Integer> sorted = lengths.stream().sorted(Comparator.naturalOrder()).toList();
        long remaining = budget;

        for (int i = 0; i < sorted.size(); i++) {
            long share = remaining / (sorted.size() - i);
            if (sorted.get(i) > share) {
                return (int) Math.max(MIN_VALUE_LENGTH, share);
            }
            remaining -= sorted.get(i);
        }

        return Integer.MAX_VALUE;
    }

    static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    static int length(Object value) {
        try {
            return MAPPER.writeValueAsString(value).length();
        } catch (JsonProcessingException e) {
            return String.valueOf(value).length();
        }
    }

    private static JsonNode resolve(JsonNode root, String path) {
        // A key containing dots, such as `metadata.source`, takes precedence over a nested path.
        if (root.isObject() && root.has(path)) {
            return root.get(path);
        }

        JsonNode node = root;
        for (String segment : path.split("\\.")) {
            if (node == null) {
                return null;
            }
            if (node.isArray() && !segment.isEmpty() && segment.length() < 10 && segment.chars().allMatch(Character::isDigit)) {
                node = node.get(Integer.parseInt(segment));
            } else {
                node = node.get(segment);
            }
        }
        return node;
    }

    private static Object toValue(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        return MAPPER.convertValue(node, Object.class);
    }
}
