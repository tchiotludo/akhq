package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record GetConnectorStatusResult(
    String cluster,
    String connect,
    String name,
    String state,
    String workerId,
    String trace,
    List<TaskStatus> tasks
) {
    @Introspected
    public record TaskStatus(int id, String state, String workerId, String trace) {
    }
}
