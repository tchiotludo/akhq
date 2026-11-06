package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSchema
@Introspected
public record ListBrokersArguments(
    @Schema(description = "Cluster name configured in AKHQ.", example = "local", requiredMode = Schema.RequiredMode.REQUIRED)
    String cluster
) implements ClusterScopedArguments {
}
