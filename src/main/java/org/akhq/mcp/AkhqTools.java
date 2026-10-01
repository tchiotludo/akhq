package org.akhq.mcp;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.AuthorizationException;
import io.micronaut.security.rules.SecurityRule;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import jakarta.inject.Singleton;
import org.akhq.mcp.model.FindMessageInTopicArguments;
import org.akhq.mcp.model.GetMessageDetailArguments;
import org.akhq.mcp.model.GetTopicLastRecordTimestampArguments;
import org.akhq.mcp.model.SearchTopicsArguments;
import org.akhq.configs.security.Role;
import org.akhq.mcp.services.AkhqTopicDataToolService;
import org.akhq.mcp.services.AkhqTopicToolService;
import org.akhq.security.annotation.AKHQSecured;

import java.util.concurrent.ExecutionException;

@Secured(SecurityRule.IS_AUTHENTICATED)
@AKHQSecured(resource = Role.Resource.TOPIC_DATA, action = Role.Action.READ)
@Singleton
@Requires(property = "akhq.mcp.enabled", value = "true")
public class AkhqTools extends AbstractMcpTool {
    private final AkhqTopicDataToolService topicDataService;
    private final AkhqTopicToolService topicService;

    public AkhqTools(AkhqTopicDataToolService topicDataService, AkhqTopicToolService topicService) {
        this.topicDataService = topicDataService;
        this.topicService = topicService;
    }

    @Tool(
        name = "akhq.search_topics",
        description = """
            Search topics of a cluster by name.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "search": "optional space separated terms",
              "maxResults": 50
            }

            Rules:
            - `cluster` is required.
            - A topic matches when its name contains every `search` term, case insensitive.
              Omit `search` to list every topic.
            - Internal topics are included.
            - `maxResults` defaults to 50 and is capped at 200. Topics are sorted by name.
            - Only topics the caller is allowed to see are returned.

            When presenting results to a user, include each topic's `name` and `partitions`. When `truncated` is true, tell the user that `totalMatches` topics
            matched and only part of them are shown.
            """
    )
    @AKHQSecured(resource = Role.Resource.TOPIC, action = Role.Action.READ)
    public CallToolResult searchTopics(SearchTopicsArguments arguments, MicronautMcpTransportContext transportContext)
        throws ExecutionException, InterruptedException {
        try {
            ClusterScope scope = authorizeClusterScope(arguments, transportContext);
            return toolResult(topicService.searchTopics(scope.cluster(), scope.resourceFilters(), arguments));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }

    @Tool(
        name = "akhq.find_message_in_topic",
        description = """
            Search topic data and return the matching messages with their values.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "topic": "<topic-name>",
              "searchByKey": "optional literal",
              "searchByKeyMatchType": "CONTAINS",
              "searchByValue": "optional literal",
              "searchByValueMatchType": "EQUALS",
              "searchByHeaderKey": "optional literal",
              "searchByHeaderKeyMatchType": "NOT_CONTAINS",
              "searchByHeaderValue": "optional literal",
              "searchByHeaderValueMatchType": "CONTAINS",
              "partition": 0,
              "timestamp": "2026-09-14T10:00:00Z",
              "endTimestamp": "2026-09-14T10:15:00Z",
              "maxMatches": 5,
              "fields": ["amount", "customer.id"],
              "after": "<nextCursor of a previous result>"
            }

            Rules:
            - `cluster` and `topic` are required.
            - Provide at least one of: `searchByKey`, `searchByValue`, `searchByHeaderKey`, `searchByHeaderValue`.
            - `timestamp` and `endTimestamp` accept ISO-8601 timestamps.
            - `maxMatches` defaults to 1 and is capped at 25, or at 500 when `fields` is set.
            - Each match includes its full `value` when the result fits the size budget. Otherwise the
              longest values are truncated and flagged with `valueTruncated: true`.
            - `fields` lists dot-separated paths to extract from JSON values, e.g. `customer.id` or
              `items.0.price`. Each match then includes a `fields` object instead of `value`, and a
              missing path is null. Use it to read or aggregate a few fields over many messages,
              instead of calling `akhq.get_message_detail` for each one.
            - When `hasMore` is true, call again with the same arguments and `after` set to
              `nextCursor` to get the next matches. Repeat until `hasMore` is false to get all of them.

            When presenting search results to a user, include each match's `partition`, `offset`,
            `timestamp`, `key`, and `value` or `fields`. Do not reduce a matching message to only its
            partition, offset, and timestamp. Use `akhq.get_message_detail` when a truncated value
            or the headers are needed.
            """
    )
    public CallToolResult findMessageInTopic(FindMessageInTopicArguments arguments, MicronautMcpTransportContext transportContext)
        throws ExecutionException, InterruptedException {
        try {
            authorizeTopicScope(arguments, transportContext);
            return toolResult(topicDataService.findMessageInTopic(arguments));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }

    @Tool(
        name = "akhq.get_message_detail",
        description = """
            Fetch one message by exact partition and offset, including full value payload and headers.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "topic": "<topic-name>",
              "partition": 0,
              "offset": 42
            }

            Rules:
            - `cluster`, `topic`, `partition`, `offset` are required.
            - `partition` and `offset` must be non-negative.

            When presenting a message to a user, preserve `headers` as an array of objects with
            `key` and `value` properties. Do not rewrite headers as a prose sentence or omit them.
            """
    )
    public CallToolResult getMessageDetail(GetMessageDetailArguments arguments, MicronautMcpTransportContext transportContext)
        throws ExecutionException, InterruptedException {
        try {
            authorizeTopicScope(arguments, transportContext);
            return toolResult(topicDataService.getMessageDetail(arguments));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }

    @Tool(
        name = "akhq.get_topic_last_record_timestamp",
        description = """
            Get the timestamp of the latest record across all partitions of a topic.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "topic": "<topic-name>"
            }

            Rules:
            - `cluster` and `topic` are required.
            - The returned timestamp is ISO-8601 UTC.
            - `found` is false and `timestamp` is null when the topic contains no records.
            """
    )
    public CallToolResult getTopicLastRecordTimestamp(
        GetTopicLastRecordTimestampArguments arguments,
        MicronautMcpTransportContext transportContext
    ) throws ExecutionException, InterruptedException {
        try {
            authorizeTopicScope(arguments, transportContext);
            return toolResult(topicDataService.getTopicLastRecordTimestamp(arguments));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }
}
