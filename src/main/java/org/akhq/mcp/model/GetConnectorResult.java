package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.Map;

@Introspected
public record GetConnectorResult(
    String cluster,
    String connect,
    String name,
    String type,
    String connectorClass,
    int taskCount,
    Map<String, String> configs
) {
}
