package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSchema
@Introspected
public record SearchConnectorsArguments(
    @Schema(description = "Cluster name configured in AKHQ.", example = "local", requiredMode = Schema.RequiredMode.REQUIRED)
    String cluster,
    @Schema(description = "Kafka Connect cluster name, as listed by akhq.list_clusters.", example = "connect-1", requiredMode = Schema.RequiredMode.REQUIRED)
    String connect,
    @Schema(description = "Optional space separated terms. A connector matches when its name contains every term, case insensitive. Omit to list everything.", example = "jdbc sink")
    String search,
    @Schema(description = "Optional task state (RUNNING, PAUSED, FAILED, UNASSIGNED). Only connectors having a task in this state are returned.", example = "FAILED")
    String status,
    @Schema(description = "Maximum number of connectors to return. Defaults to 50, max 200.", example = "50")
    Integer maxResults
) implements ClusterScopedArguments {
}
