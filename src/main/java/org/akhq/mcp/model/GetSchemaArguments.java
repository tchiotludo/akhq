package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSchema
@Introspected
public record GetSchemaArguments(
    @Schema(description = "Cluster name configured in AKHQ.", example = "local", requiredMode = Schema.RequiredMode.REQUIRED)
    String cluster,
    @Schema(description = "Schema registry subject.", example = "orders-value", requiredMode = Schema.RequiredMode.REQUIRED)
    String subject,
    @Schema(description = "Optional schema version. Defaults to the latest version.", example = "3")
    Integer version
) implements ClusterScopedArguments {
}
