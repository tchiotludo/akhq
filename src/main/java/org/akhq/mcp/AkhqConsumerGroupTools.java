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
import org.akhq.mcp.services.AkhqConsumerGroupToolService;
import org.akhq.security.annotation.AKHQSecured;

@Secured(SecurityRule.IS_AUTHENTICATED)
@AKHQSecured(resource = Role.Resource.CONSUMER_GROUP, action = Role.Action.READ)
@Singleton
@Requires(property = "akhq.mcp.enabled", value = "true")
public class AkhqConsumerGroupTools extends AbstractMcpTool {
    private final AkhqConsumerGroupToolService service;

    public AkhqConsumerGroupTools(AkhqConsumerGroupToolService service) {
        this.service = service;
    }

    @Tool(
        name = "akhq.search_consumer_groups",
        description = """
            Search the consumer groups of a cluster by name.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "search": "optional space separated terms",
              "maxResults": 50
            }

            Rules:
            - `cluster` is required.
            - A group matches when its id contains every `search` term, case insensitive. Omit `search` to list every group.
            - `maxResults` defaults to 50 and is capped at 200. Groups are sorted by id.
            - Each group has its `state`, `memberCount`, `topics` and `totalLag`. Only groups the caller is allowed to see are returned.

            When `truncated` is true, tell the user that `totalMatches` groups matched and only part of them are shown.
            """
    )
    public CallToolResult searchConsumerGroups(SearchConsumerGroupsArguments arguments, MicronautMcpTransportContext transportContext) throws Exception {
        try {
            ClusterScope scope = authorizeClusterScope(arguments, transportContext);
            return toolResult(service.searchConsumerGroups(scope.cluster(), scope.resourceFilters(), arguments.search(), arguments.maxResults()));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }

    @Tool(
        name = "akhq.get_consumer_group",
        description = """
            Get the details of one consumer group: state, assignor, coordinator, member count and the lag of each topic.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "group": "<group-id>"
            }

            Rules:
            - `cluster` and `group` are required.
            - Use `akhq.get_consumer_group_offsets` for the per partition offsets and `akhq.get_consumer_group_members` for the members.
            """
    )
    public CallToolResult getConsumerGroup(GetConsumerGroupArguments arguments, MicronautMcpTransportContext transportContext) throws Exception {
        try {
            ResourceScope scope = authorizeResourceScope(arguments, transportContext, arguments.group(), "group");
            return toolResult(service.getConsumerGroup(scope.cluster(), scope.resourceFilters(), scope.resource()));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }

    @Tool(
        name = "akhq.get_consumer_group_offsets",
        description = """
            Get the committed offsets of a consumer group per topic partition, with the first and last offsets of the partition and the lag.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "group": "<group-id>",
              "topic": "optional topic name",
              "maxResults": 100
            }

            Rules:
            - `cluster` and `group` are required.
            - `topic` restricts the result to one topic.
            - `maxResults` defaults to 100 and is capped at 500. Offsets are sorted by topic and partition.
            - `offset` and `lag` are null when the group has no committed offset on the partition.

            When `truncated` is true, tell the user that `totalOffsets` offsets exist and only part of them are shown.
            """
    )
    public CallToolResult getConsumerGroupOffsets(GetConsumerGroupOffsetsArguments arguments, MicronautMcpTransportContext transportContext) throws Exception {
        try {
            ResourceScope scope = authorizeResourceScope(arguments, transportContext, arguments.group(), "group");
            return toolResult(service.getOffsets(scope.cluster(), scope.resourceFilters(), scope.resource(), arguments.topic(), arguments.maxResults()));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }

    @Tool(
        name = "akhq.get_consumer_group_members",
        description = """
            Get the active members of a consumer group with their client id, host and assigned partitions per topic.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "group": "<group-id>",
              "maxResults": 50
            }

            Rules:
            - `cluster` and `group` are required.
            - `maxResults` defaults to 50 and is capped at 200.
            - A group without active consumer has no member.
            """
    )
    public CallToolResult getConsumerGroupMembers(GetConsumerGroupMembersArguments arguments, MicronautMcpTransportContext transportContext) throws Exception {
        try {
            ResourceScope scope = authorizeResourceScope(arguments, transportContext, arguments.group(), "group");
            return toolResult(service.getMembers(scope.cluster(), scope.resourceFilters(), scope.resource(), arguments.maxResults()));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }

    @Tool(
        name = "akhq.get_consumer_group_lag",
        description = """
            Get the lag of a consumer group: total lag, lag per topic and the most lagging partitions.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "group": "<consumer-group-id>",
              "topic": "optional topic name",
              "maxResults": 20
            }

            Rules:
            - `cluster` and `group` are required.
            - `topic` restricts the lag to one topic.
            - `partitions` only lists lagging partitions (lag > 0), the most lagging first. `maxResults` defaults to 20 and is capped at 200.
            - Use `akhq.get_consumer_group_offsets` for the offsets of every partition.
            """
    )
    public CallToolResult getConsumerGroupLag(GetConsumerGroupLagArguments arguments, MicronautMcpTransportContext transportContext) throws Exception {
        try {
            ResourceScope scope = authorizeResourceScope(arguments, transportContext, arguments.group(), "group");
            return toolResult(service.getLag(scope.cluster(), scope.resourceFilters(), scope.resource(), arguments.topic(), arguments.maxResults()));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }

    @Tool(
        name = "akhq.get_topic_consumer_groups",
        description = """
            List the consumer groups consuming a topic, with their state and their lag on this topic.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "topic": "<topic-name>",
              "onlyActive": false,
              "maxResults": 50
            }

            Rules:
            - `cluster` and `topic` are required. Only consumer groups the caller is allowed to see are returned.
            - `onlyActive` restricts to groups with an active member; it is much faster on clusters with many groups, because the default also scans empty groups.
            - `maxResults` defaults to 50 and is capped at 200. Groups are sorted by id.
            - Use `akhq.get_consumer_group_lag` for the lag detail of a group.
            """
    )
    public CallToolResult getTopicConsumerGroups(GetTopicConsumerGroupsArguments arguments, MicronautMcpTransportContext transportContext) throws Exception {
        try {
            ClusterScope scope = authorizeClusterScope(arguments, transportContext);
            if (arguments.topic() == null || arguments.topic().isBlank()) {
                throw new IllegalArgumentException("`arguments.topic` is required");
            }
            return toolResult(service.getTopicConsumerGroups(
                scope.cluster(), scope.resourceFilters(), arguments.topic().trim(), arguments.onlyActive(), arguments.maxResults()
            ));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }
}
