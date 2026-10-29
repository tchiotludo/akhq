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
import org.akhq.mcp.model.GetTopicArguments;
import org.akhq.mcp.model.GetTopicConfigsArguments;
import org.akhq.mcp.services.AkhqTopicToolService;
import org.akhq.security.annotation.AKHQSecured;

@Secured(SecurityRule.IS_AUTHENTICATED)
@AKHQSecured(resource = Role.Resource.TOPIC, action = Role.Action.READ)
@Singleton
@Requires(property = "akhq.mcp.enabled", value = "true")
public class AkhqTopicDetailTools extends AbstractMcpTool {
    private final AkhqTopicToolService service;

    public AkhqTopicDetailTools(AkhqTopicToolService service) {
        this.service = service;
    }

    @Tool(
        name = "akhq.get_topic",
        description = """
            Get the details of a topic: partitions with their leader, replicas, in-sync state, first and last offsets,
            record count and size on disk.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "topic": "<topic-name>",
              "maxResults": 100
            }

            Rules:
            - `cluster` and `topic` are required.
            - `maxResults` is the maximum number of partitions returned, default 100, capped at 500. Partitions are sorted by id.
            - `underReplicatedPartitions` counts the partitions with a replica out of sync.
            - Use `akhq.get_topic_configs` for the topic configuration.
            """
    )
    public CallToolResult getTopic(GetTopicArguments arguments, MicronautMcpTransportContext transportContext) throws Exception {
        try {
            TopicScope scope = authorizeTopicScope(arguments, transportContext);
            return toolResult(service.getTopic(scope.cluster(), scope.topic(), arguments.maxResults()));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }

    @Tool(
        name = "akhq.get_topic_configs",
        description = """
            Get the configuration of a topic (retention, cleanup policy, ...).

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "topic": "<topic-name>",
              "search": "optional space separated terms",
              "onlyOverridden": false,
              "maxResults": 100
            }

            Rules:
            - `cluster` and `topic` are required. Requires the permission to read topic configs.
            - A config matches when its name contains every `search` term, case insensitive.
            - `onlyOverridden` keeps only the configs set on the topic, not the broker defaults.
            - `maxResults` defaults to 100 and is capped at 500.
            - The value of a sensitive config is always omitted (`sensitive: true`).
            """
    )
    @AKHQSecured(resource = Role.Resource.TOPIC, action = Role.Action.READ_CONFIG)
    public CallToolResult getTopicConfigs(GetTopicConfigsArguments arguments, MicronautMcpTransportContext transportContext) throws Exception {
        try {
            TopicScope scope = authorizeTopicScope(arguments, transportContext);
            return toolResult(service.getTopicConfigs(
                scope.cluster(), scope.topic(), arguments.search(), arguments.onlyOverridden(), arguments.maxResults()
            ));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }
}
