package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;

@JsonSchema
@Introspected
public record GetTopicLastRecordTimestampResult(
    boolean found,
    String topic,
    String timestamp,
    String message
) {
}
