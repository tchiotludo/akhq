package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSchema
@Introspected
public record GetTopicConfigsArguments(
    @Schema(description = "Cluster name configured in AKHQ.", example = "local", requiredMode = Schema.RequiredMode.REQUIRED)
    String cluster,
    @Schema(description = "Kafka topic name.", example = "orders", requiredMode = Schema.RequiredMode.REQUIRED)
    String topic,
    @Schema(description = "Optional space separated terms. A config matches when its name contains every term, case insensitive.", example = "retention")
    String search,
    @Schema(description = "When true, only return the configs explicitly set, not the defaults. Defaults to false.", example = "true")
    Boolean onlyOverridden,
    @Schema(description = "Maximum number of configs to return. Defaults to 100, max 500.", example = "100")
    Integer maxResults
) implements TopicScopedArguments {
}
