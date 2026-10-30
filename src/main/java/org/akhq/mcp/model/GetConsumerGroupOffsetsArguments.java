package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSchema
@Introspected
public record GetConsumerGroupOffsetsArguments(
    @Schema(description = "Cluster name configured in AKHQ.", example = "local", requiredMode = Schema.RequiredMode.REQUIRED)
    String cluster,
    @Schema(description = "Consumer group id.", example = "orders-service", requiredMode = Schema.RequiredMode.REQUIRED)
    String group,
    @Schema(description = "Optional topic name, to only return the offsets of this topic.", example = "orders")
    String topic,
    @Schema(description = "Maximum number of partition offsets to return. Defaults to 100, max 500.", example = "100")
    Integer maxResults
) implements ClusterScopedArguments {
}
