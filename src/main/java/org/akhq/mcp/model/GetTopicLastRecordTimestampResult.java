package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record GetTopicLastRecordTimestampResult(
    boolean found,
    String topic,
    String timestamp,
    String message
) {
}
