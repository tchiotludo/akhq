package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record GetTopicResult(
    String cluster,
    String name,
    boolean internal,
    int partitionCount,
    long replicationFactor,
    int underReplicatedPartitions,
    long totalRecords,
    Long sizeBytes,
    boolean truncated,
    List<Partition> partitions,
    String message
) {
    @Introspected
    public record Partition(
        int id,
        int leader,
        long firstOffset,
        long lastOffset,
        long records,
        long sizeBytes,
        List<Replica> replicas
    ) {
    }

    @Introspected
    public record Replica(int brokerId, boolean leader, boolean inSync) {
    }
}
