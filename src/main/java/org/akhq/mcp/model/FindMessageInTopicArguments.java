package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSchema
@Introspected
public record FindMessageInTopicArguments(
    @Schema(description = "Cluster name configured in AKHQ.", example = "local", requiredMode = Schema.RequiredMode.REQUIRED)
    String cluster,
    @Schema(description = "Kafka topic name to search.", example = "orders", requiredMode = Schema.RequiredMode.REQUIRED)
    String topic,
    @Schema(description = "Literal filter applied to the message key.", example = "customer-42")
    String searchByKey,
    @Schema(description = "How to match searchByKey. Defaults to CONTAINS.")
    SearchMatchType searchByKeyMatchType,
    @Schema(description = "Literal filter applied to the message value.", example = "FAILED")
    String searchByValue,
    @Schema(description = "How to match searchByValue. Defaults to CONTAINS.")
    SearchMatchType searchByValueMatchType,
    @Schema(description = "Literal filter applied to header keys.", example = "traceId")
    String searchByHeaderKey,
    @Schema(description = "How to match searchByHeaderKey. Defaults to CONTAINS.")
    SearchMatchType searchByHeaderKeyMatchType,
    @Schema(description = "Literal filter applied to header values.", example = "checkout")
    String searchByHeaderValue,
    @Schema(description = "How to match searchByHeaderValue. Defaults to CONTAINS.")
    SearchMatchType searchByHeaderValueMatchType,
    @Schema(description = "Optional partition to restrict the search.", example = "0")
    Integer partition,
    @Schema(description = "Inclusive start timestamp in ISO-8601 format.", example = "2026-09-14T10:00:00Z")
    String timestamp,
    @Schema(description = "Inclusive end timestamp in ISO-8601 format.", example = "2026-09-14T10:15:00Z")
    String endTimestamp,
    @Schema(description = "Maximum number of matches to return. Defaults to 1, max 25.", example = "5")
    Integer maxMatches
) {
}
