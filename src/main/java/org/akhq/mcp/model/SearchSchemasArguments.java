package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSchema
@Introspected
public record SearchSchemasArguments(
    @Schema(description = "Cluster name configured in AKHQ.", example = "local", requiredMode = Schema.RequiredMode.REQUIRED)
    String cluster,
    @Schema(description = "Optional space separated terms. A subject matches when its name contains every term, case insensitive. Omit to list everything.", example = "orders-value")
    String search,
    @Schema(description = "Maximum number of subjects to return. Defaults to 50, max 200.", example = "50")
    Integer maxResults
) implements ClusterScopedArguments {
}
