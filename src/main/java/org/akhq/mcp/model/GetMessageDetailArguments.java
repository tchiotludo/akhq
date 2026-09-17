package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSchema
@Introspected
public record GetMessageDetailArguments(
    @Schema(description = "Cluster name configured in AKHQ.", example = "local", requiredMode = Schema.RequiredMode.REQUIRED)
    String cluster,
    @Schema(description = "Kafka topic name.", example = "orders", requiredMode = Schema.RequiredMode.REQUIRED)
    String topic,
    @Schema(description = "Partition id.", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    Integer partition,
    @Schema(description = "Exact offset to fetch.", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    Long offset
) {
}

