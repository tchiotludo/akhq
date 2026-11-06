package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSchema
@Introspected
public record GetConnectorArguments(
    @Schema(description = "Cluster name configured in AKHQ.", example = "local", requiredMode = Schema.RequiredMode.REQUIRED)
    String cluster,
    @Schema(description = "Kafka Connect cluster name, as listed by akhq.list_clusters.", example = "connect-1", requiredMode = Schema.RequiredMode.REQUIRED)
    String connect,
    @Schema(description = "Connector name.", example = "jdbc-sink", requiredMode = Schema.RequiredMode.REQUIRED)
    String name
) implements ClusterScopedArguments {
}
