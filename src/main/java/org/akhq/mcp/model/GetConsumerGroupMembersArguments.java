package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSchema
@Introspected
public record GetConsumerGroupMembersArguments(
    @Schema(description = "Cluster name configured in AKHQ.", example = "local", requiredMode = Schema.RequiredMode.REQUIRED)
    String cluster,
    @Schema(description = "Consumer group id.", example = "orders-service", requiredMode = Schema.RequiredMode.REQUIRED)
    String group,
    @Schema(description = "Maximum number of members to return. Defaults to 50, max 200.", example = "50")
    Integer maxResults
) implements ClusterScopedArguments {
}
