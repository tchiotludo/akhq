package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record SearchTopicsResult(
    String cluster,
    int totalMatches,
    boolean truncated,
    List<TopicOverview> topics,
    String message
) {
}
