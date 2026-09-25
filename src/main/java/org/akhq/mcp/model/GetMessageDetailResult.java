package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;

import java.util.List;

@JsonSchema
@Introspected
public record GetMessageDetailResult(
    boolean found,
    String topic,
    Integer partition,
    Long offset,
    String timestamp,
    String key,
    String value,
    List<MessageHeader> headers,
    String message
) {
}

