package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record FindSchemasForTopicResult(
    String cluster,
    String topic,
    GetSchemaResult keySchema,
    GetSchemaResult valueSchema,
    List<String> otherSubjects,
    String message
) {
}
