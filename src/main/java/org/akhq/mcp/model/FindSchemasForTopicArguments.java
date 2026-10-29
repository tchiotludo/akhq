package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSchema
@Introspected
public record FindSchemasForTopicArguments(
    @Schema(description = "Cluster name configured in AKHQ.", example = "local", requiredMode = Schema.RequiredMode.REQUIRED)
    String cluster,
    @Schema(description = "Kafka topic name. Its key and value schemas are looked up with the default TopicNameStrategy (`<topic>-key`, `<topic>-value`).", example = "orders", requiredMode = Schema.RequiredMode.REQUIRED)
    String topic
) implements ClusterScopedArguments {
}
