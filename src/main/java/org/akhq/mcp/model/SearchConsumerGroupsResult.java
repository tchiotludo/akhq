package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record SearchConsumerGroupsResult(
    String cluster,
    int totalMatches,
    boolean truncated,
    List<ConsumerGroupOverview> groups,
    String message
) {
    @Introspected
    public record ConsumerGroupOverview(
        String id,
        String state,
        String partitionAssignor,
        int memberCount,
        List<String> topics,
        long totalLag
    ) {
    }
}
