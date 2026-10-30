package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record ListClustersResult(
    List<ClusterOverview> clusters,
    String message
) {
    @Introspected
    public record ClusterOverview(
        String name,
        boolean schemaRegistry,
        String schemaRegistryType,
        List<String> connects,
        List<String> ksqldbs
    ) {
    }
}
