package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSchema
@Introspected
public record GetTopicArguments(
    @Schema(description = "Cluster name configured in AKHQ.", example = "local", requiredMode = Schema.RequiredMode.REQUIRED)
    String cluster,
    @Schema(description = "Kafka topic name.", example = "orders", requiredMode = Schema.RequiredMode.REQUIRED)
    String topic,
    @Schema(description = "Maximum number of partitions to return. Defaults to 100, max 500.", example = "100")
    Integer maxResults
) implements TopicScopedArguments {
}
