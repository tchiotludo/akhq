package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSchema
@Introspected
public record GetSchemaByIdArguments(
    @Schema(description = "Cluster name configured in AKHQ.", example = "local", requiredMode = Schema.RequiredMode.REQUIRED)
    String cluster,
    @Schema(description = "Global schema id, as found in the messages wire format.", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    Integer id,
    @Schema(description = "Optional topic name, to prefer the subject matching this topic when several subjects share the schema.", example = "orders")
    String topic
) implements ClusterScopedArguments {
}
