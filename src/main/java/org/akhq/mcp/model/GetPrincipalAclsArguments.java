package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSchema
@Introspected
public record GetPrincipalAclsArguments(
    @Schema(description = "Cluster name configured in AKHQ.", example = "local", requiredMode = Schema.RequiredMode.REQUIRED)
    String cluster,
    @Schema(description = "Exact principal, including its type.", example = "User:alice", requiredMode = Schema.RequiredMode.REQUIRED)
    String principal,
    @Schema(description = "Optional resource type (TOPIC, GROUP, CLUSTER, TRANSACTIONAL_ID, DELEGATION_TOKEN, USER) to restrict the ACLs.", example = "TOPIC")
    String resourceType,
    @Schema(description = "Maximum number of ACLs to return. Defaults to 100, max 500.", example = "100")
    Integer maxResults
) implements ClusterScopedArguments {
}
