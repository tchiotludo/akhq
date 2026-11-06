package org.akhq.mcp.services;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageValuesTest {
    private static final String ORDER = """
        {
          "amount": 12.5,
          "customer": {"id": "c-1", "vip": true},
          "items": [{"price": 3}, {"price": 9.5}],
          "metadata.source": "web",
          "metadata": {"source": "nested"}
        }
        """;

    @Test
    void projectsNestedArrayAndMissingPaths() {
        Map<String, Object> projection = MessageValues.project(
            ORDER,
            List.of("amount", "customer.id", "items.1.price", "customer", "missing.path", "items.5.price")
        ).orElseThrow();

        Map<String, Object> expected = new HashMap<>();
        expected.put("amount", 12.5);
        expected.put("customer.id", "c-1");
        expected.put("items.1.price", 9.5);
        expected.put("customer", Map.of("id", "c-1", "vip", true));
        expected.put("missing.path", null);
        expected.put("items.5.price", null);
        assertEquals(expected, projection);
        assertEquals(List.of("amount", "customer.id", "items.1.price", "customer", "missing.path", "items.5.price"),
            List.copyOf(projection.keySet()));
    }

    @Test
    void literalDottedKeyTakesPrecedence() {
        assertEquals("web", MessageValues.project(ORDER, List.of("metadata.source")).orElseThrow().get("metadata.source"));
    }

    @Test
    void projectsTopLevelArrays() {
        assertEquals(2, MessageValues.project("[1, 2]", List.of("1")).orElseThrow().get("1"));
    }

    @Test
    void doesNotProjectNonJsonValues() {
        assertEquals(Optional.empty(), MessageValues.project(null, List.of("a")));
        assertEquals(Optional.empty(), MessageValues.project("value_42", List.of("a")));
        assertEquals(Optional.empty(), MessageValues.project("42", List.of("a")));
        assertEquals(Optional.empty(), MessageValues.project("\"text\"", List.of("a")));
        assertEquals(Optional.empty(), MessageValues.project("{broken", List.of("a")));
    }

    @Test
    void maxValueLengthKeepsEverythingWhenItFits() {
        assertEquals(Integer.MAX_VALUE, MessageValues.maxValueLength(List.of(10, 20, 30), 60));
        assertEquals(Integer.MAX_VALUE, MessageValues.maxValueLength(List.of(), 0));
    }

    @Test
    void maxValueLengthSharesTheBudgetLeftByShortValues() {
        // 2 short values use 1_000, the 2 long ones share the 9_000 left.
        assertEquals(4_500, MessageValues.maxValueLength(List.of(500, 50_000, 500, 80_000), 10_000));
    }

    @Test
    void maxValueLengthNeverGoesBelowTheMinimum() {
        Integer[] lengths = new Integer[1_000];
        Arrays.fill(lengths, 10_000);
        assertEquals(MessageValues.MIN_VALUE_LENGTH, MessageValues.maxValueLength(List.of(lengths), 1_000));
    }

    @Test
    void truncatesLongValuesOnly() {
        assertEquals("short  value", MessageValues.truncate("short  value", 20));
        assertEquals(null, MessageValues.truncate(null, 20));

        String truncated = MessageValues.truncate("a  b\n c " + "x".repeat(50), 10);
        assertEquals("a b c xxxx...", truncated);
        assertTrue(truncated.length() <= 13);
    }

    @Test
    void lengthIsTheJsonLength() {
        assertEquals(13, MessageValues.length(Map.of("a", "value")));
        assertEquals(4, MessageValues.length(null));
    }
}
