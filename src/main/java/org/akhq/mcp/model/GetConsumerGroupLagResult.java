package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record GetConsumerGroupLagResult(
    String cluster,
    String group,
    String state,
    long totalLag,
    List<TopicLag> topics,
    int totalLaggingPartitions,
    boolean truncated,
    List<PartitionLag> partitions,
    String message
) {
    @Introspected
    public record TopicLag(String topic, long lag, int laggingPartitions) {
    }

    @Introspected
    public record PartitionLag(String topic, int partition, Long offset, Long endOffset, long lag) {
    }
}
