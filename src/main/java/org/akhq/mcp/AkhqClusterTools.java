package org.akhq.mcp;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.AuthorizationException;
import io.micronaut.security.rules.SecurityRule;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import jakarta.inject.Singleton;
import org.akhq.configs.security.Role;
import org.akhq.mcp.model.*;
import org.akhq.mcp.services.AkhqClusterToolService;
import org.akhq.security.annotation.AKHQSecured;

@Secured(SecurityRule.IS_AUTHENTICATED)
@AKHQSecured(resource = Role.Resource.TOPIC, action = Role.Action.READ)
@Singleton
@Requires(property = "akhq.mcp.enabled", value = "true")
public class AkhqClusterTools extends AbstractMcpTool {
    private final AkhqClusterToolService service;

    public AkhqClusterTools(AkhqClusterToolService service) {
        this.service = service;
    }

    @Tool(
        name = "akhq.list_clusters",
        description = """
            List the Kafka clusters the caller can access, with the optional Schema Registry, Kafka Connect and ksqlDB
            clusters configured on each.

            Expected `arguments` JSON object: {}

            Use each cluster `name` as the `cluster` argument of the other tools, and each name of `connects` as the
            `connect` argument of the connector tools. `schemaRegistry` is false when the cluster has no Schema Registry.
            """
    )
    public CallToolResult listClusters(ListClustersArguments arguments, MicronautMcpTransportContext transportContext) {
        if (transportContext == null) {
            return toolError(new IllegalArgumentException("MCP transport context is required"));
        }

        return toolResult(service.listClusters());
    }
}
