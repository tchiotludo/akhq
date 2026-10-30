package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record GetConfigsResult(
    String cluster,
    String resourceType,
    String resource,
    int totalConfigs,
    boolean truncated,
    List<ConfigEntry> configs,
    String message
) {
    @Introspected
    public record ConfigEntry(String name, String value, String source, boolean readOnly, boolean sensitive) {
    }
}
