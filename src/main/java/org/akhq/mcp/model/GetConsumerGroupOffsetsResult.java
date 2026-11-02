package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record GetConsumerGroupOffsetsResult(
    String cluster,
    String group,
    int totalOffsets,
    boolean truncated,
    List<PartitionOffset> offsets,
    String message
) {
    @Introspected
    public record PartitionOffset(
        String topic,
        int partition,
        Long offset,
        Long firstOffset,
        Long lastOffset,
        Long lag
    ) {
    }
}
