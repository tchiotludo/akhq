package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record TopicOverview(
    String name,
    int partitions
) {
}
