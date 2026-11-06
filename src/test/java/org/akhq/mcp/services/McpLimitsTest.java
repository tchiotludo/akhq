package org.akhq.mcp.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpLimitsTest {
    @Test
    void appliesDefaultsAndCaps() {
        assertEquals(50, McpLimits.maxResults(null));
        assertEquals(200, McpLimits.maxResults(10_000));
        assertEquals(100, McpLimits.maxItems(null));
        assertEquals(500, McpLimits.maxItems(10_000));
        assertEquals(7, McpLimits.maxItems(7));
        assertThrows(IllegalArgumentException.class, () -> McpLimits.maxResults(0));
    }

    @Test
    void truncatesLongText() {
        assertEquals("ab", McpLimits.truncate("abc", 2));
        assertEquals("abc", McpLimits.truncate("abc", 3));
    }
}
