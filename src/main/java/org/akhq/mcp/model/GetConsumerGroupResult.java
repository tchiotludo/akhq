package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record GetConsumerGroupResult(
    String cluster,
    String id,
    boolean simpleConsumerGroup,
    String state,
    String partitionAssignor,
    Coordinator coordinator,
    int memberCount,
    long totalLag,
    List<TopicLag> topics
) {
    @Introspected
    public record Coordinator(int id, String host, int port, String rack) {
    }

    @Introspected
    public record TopicLag(String topic, boolean active, long lag) {
    }
}
