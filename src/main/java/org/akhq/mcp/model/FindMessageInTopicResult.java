package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record FindMessageInTopicResult(
    boolean found,
    String topic,
    int matchCount,
    List<MessageOverview> messages,
    String message,
    TimeWindowSuggestion timeWindowSuggestion
) {
}
