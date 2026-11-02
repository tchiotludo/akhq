package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSchema
@Introspected
public record GetTopicConsumerGroupsArguments(
    @Schema(description = "Cluster name configured in AKHQ.", example = "local", requiredMode = Schema.RequiredMode.REQUIRED)
    String cluster,
    @Schema(description = "Kafka topic name.", example = "orders", requiredMode = Schema.RequiredMode.REQUIRED)
    String topic,
    @Schema(description = "When true, only return groups with an active member. Defaults to false, which also scans empty groups and is slower.", example = "true")
    Boolean onlyActive,
    @Schema(description = "Maximum number of groups to return. Defaults to 50, max 200.", example = "50")
    Integer maxResults
) implements ClusterScopedArguments {
}
