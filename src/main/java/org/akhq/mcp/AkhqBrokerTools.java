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
import org.akhq.mcp.model.GetBrokerConfigsArguments;
import org.akhq.mcp.model.ListBrokersArguments;
import org.akhq.mcp.services.AkhqBrokerToolService;
import org.akhq.security.annotation.AKHQSecured;

@Secured(SecurityRule.IS_AUTHENTICATED)
@AKHQSecured(resource = Role.Resource.NODE, action = Role.Action.READ)
@Singleton
@Requires(property = "akhq.mcp.enabled", value = "true")
public class AkhqBrokerTools extends AbstractMcpTool {
    private final AkhqBrokerToolService service;

    public AkhqBrokerTools(AkhqBrokerToolService service) {
        this.service = service;
    }

    @Tool(
        name = "akhq.list_brokers",
        description = """
            List the brokers of a cluster: id, host, port, rack and which one is the controller.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>"
            }

            Rules:
            - `cluster` is required.
            - Use the broker ids with `akhq.get_broker_configs`.
            """
    )
    public CallToolResult listBrokers(ListBrokersArguments arguments, MicronautMcpTransportContext transportContext) throws Exception {
        try {
            ClusterScope scope = authorizeClusterScope(arguments, transportContext);
            return toolResult(service.listBrokers(scope.cluster()));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }

    @Tool(
        name = "akhq.get_broker_configs",
        description = """
            Get the configuration of one broker. Kafka has no cluster-wide config: the broker configs are the cluster configs,
            so query one broker (see `akhq.list_brokers`) to know the cluster settings.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "brokerId": 1,
              "search": "optional space separated terms",
              "onlyOverridden": false,
              "maxResults": 100
            }

            Rules:
            - `cluster` and `brokerId` are required. Requires the permission to read node configs.
            - A config matches when its name contains every `search` term, case insensitive.
            - `onlyOverridden` keeps only the dynamically set configs, not the static or default ones.
            - `maxResults` defaults to 100 and is capped at 500.
            - The value of a sensitive config is always omitted (`sensitive: true`).
            """
    )
    @AKHQSecured(resource = Role.Resource.NODE, action = Role.Action.READ_CONFIG)
    public CallToolResult getBrokerConfigs(GetBrokerConfigsArguments arguments, MicronautMcpTransportContext transportContext) throws Exception {
        try {
            String broker = arguments == null || arguments.brokerId() == null ? null : arguments.brokerId().toString();
            ResourceScope scope = authorizeResourceScope(arguments, transportContext, broker, "brokerId");
            return toolResult(service.getBrokerConfigs(
                scope.cluster(), arguments.brokerId(), arguments.search(), arguments.onlyOverridden(), arguments.maxResults()
            ));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }
}
