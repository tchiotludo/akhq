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
import org.akhq.mcp.services.AkhqConnectToolService;
import org.akhq.security.annotation.AKHQSecured;

@Secured(SecurityRule.IS_AUTHENTICATED)
@AKHQSecured(resource = Role.Resource.CONNECTOR, action = Role.Action.READ)
@Singleton
@Requires(property = "akhq.mcp.enabled", value = "true")
public class AkhqConnectTools extends AbstractMcpTool {
    private final AkhqConnectToolService service;

    public AkhqConnectTools(AkhqConnectToolService service) {
        this.service = service;
    }

    @Tool(
        name = "akhq.search_connectors",
        description = """
            Search the connectors of a Kafka Connect cluster by name or task state.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "connect": "<connect-name>",
              "search": "optional space separated terms",
              "status": "FAILED",
              "maxResults": 50
            }

            Rules:
            - `cluster` and `connect` are required. Get the `connect` names with `akhq.list_clusters`.
            - A connector matches when its name contains every `search` term, case insensitive.
            - `status` keeps only the connectors having a task in this state (RUNNING, PAUSED, FAILED, UNASSIGNED).
            - `maxResults` defaults to 50 and is capped at 200. Connectors are sorted by name.
            - Only connectors the caller is allowed to see are returned.

            When `truncated` is true, tell the user that `totalMatches` connectors matched and only part of them are shown.
            """
    )
    public CallToolResult searchConnectors(SearchConnectorsArguments arguments, MicronautMcpTransportContext transportContext) throws Exception {
        try {
            ClusterScope scope = authorizeClusterScope(arguments, transportContext);
            return toolResult(service.searchConnectors(scope.cluster(), scope.resourceFilters(), arguments.connect(), arguments.search(), arguments.status(), arguments.maxResults()));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }

    @Tool(
        name = "akhq.get_connector",
        description = """
            Get a connector: its type, connector class, task count and configuration.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "connect": "<connect-name>",
              "name": "<connector-name>"
            }

            Rules:
            - `cluster`, `connect` and `name` are required.
            - `configs` is returned as Kafka Connect exposes it, so treat its values as sensitive and do not repeat credentials to the user.
            - Use `akhq.get_connector_status` for the connector and task states.
            """
    )
    public CallToolResult getConnector(GetConnectorArguments arguments, MicronautMcpTransportContext transportContext) throws Exception {
        try {
            ResourceScope scope = authorizeResourceScope(arguments, transportContext, arguments.name(), "name");
            return toolResult(service.getConnector(scope.cluster(), arguments.connect(), scope.resource()));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }

    @Tool(
        name = "akhq.get_connector_status",
        description = """
            Get the runtime status of a connector: its state and the state of each task, with the worker and the failure trace if any.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "connect": "<connect-name>",
              "name": "<connector-name>"
            }

            Rules:
            - `cluster`, `connect` and `name` are required.
            - States are RUNNING, PAUSED, FAILED or UNASSIGNED. `trace` is only set on failure and is cut at 2000 characters.
            """
    )
    public CallToolResult getConnectorStatus(GetConnectorArguments arguments, MicronautMcpTransportContext transportContext) throws Exception {
        try {
            ResourceScope scope = authorizeResourceScope(arguments, transportContext, arguments.name(), "name");
            return toolResult(service.getConnectorStatus(scope.cluster(), arguments.connect(), scope.resource()));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }
}
