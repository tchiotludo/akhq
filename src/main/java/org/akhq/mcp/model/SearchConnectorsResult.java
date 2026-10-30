package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;
import java.util.Map;

@Introspected
public record SearchConnectorsResult(
    String cluster,
    String connect,
    int totalMatches,
    boolean truncated,
    List<ConnectorOverview> connectors,
    String message
) {
    @Introspected
    public record ConnectorOverview(
        String name,
        String type,
        String connectorClass,
        int taskCount,
        Map<String, Long> taskStates
    ) {
    }
}
