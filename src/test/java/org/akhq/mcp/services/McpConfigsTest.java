package org.akhq.mcp.services;

import org.akhq.mcp.model.GetConfigsResult;
import org.akhq.models.Config;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpConfigsTest {
    private static Config config(String name, String value, Config.Source source, boolean sensitive) {
        return new Config(name, value, null, source, sensitive, false);
    }

    private final List<Config> configs = List.of(
        config("retention.ms", "1000", Config.Source.DYNAMIC_TOPIC_CONFIG, false),
        config("retention.bytes", "-1", Config.Source.DEFAULT_CONFIG, false),
        config("ssl.key.password", "hunter2", Config.Source.DYNAMIC_BROKER_CONFIG, true)
    );

    @Test
    void filtersBySearchAndOverridden() {
        GetConfigsResult result = McpConfigs.result("local", "TOPIC", "orders", configs, "RETENTION", true, null);

        assertEquals(1, result.totalConfigs());
        assertEquals("retention.ms", result.configs().get(0).name());
    }

    @Test
    void neverReturnsSensitiveValues() {
        GetConfigsResult result = McpConfigs.result("local", "BROKER", "1", configs, "password", null, null);

        assertNull(result.configs().get(0).value());
        assertTrue(result.configs().get(0).sensitive());
    }

    @Test
    void boundsTheResult() {
        GetConfigsResult result = McpConfigs.result("local", "TOPIC", "orders", configs, null, null, 2);

        assertTrue(result.truncated());
        assertEquals(3, result.totalConfigs());
        assertEquals(2, result.configs().size());
    }
}
