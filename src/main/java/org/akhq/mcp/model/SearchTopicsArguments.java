package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSchema
@Introspected
public record SearchTopicsArguments(
    @Schema(description = "Cluster name configured in AKHQ.", example = "local", requiredMode = Schema.RequiredMode.REQUIRED)
    String cluster,
    @Schema(
        description = "Optional space separated terms. A topic matches when its name contains every term, case insensitive. Omit to list every topic.",
        example = "orders payment"
    )
    String search,
    @Schema(description = "Maximum number of topics to return. Defaults to 50, max 200.", example = "50")
    Integer maxResults
) implements ClusterScopedArguments {
}
