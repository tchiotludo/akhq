package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record ListBrokersResult(
    String cluster,
    String clusterId,
    Integer controllerId,
    List<Broker> brokers,
    String message
) {
    @Introspected
    public record Broker(int id, String host, int port, String rack, boolean controller) {
    }
}
