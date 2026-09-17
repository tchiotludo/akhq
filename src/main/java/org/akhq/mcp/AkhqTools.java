package org.akhq.mcp;

import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Singleton;
import org.akhq.controllers.AbstractController;
import org.akhq.mcp.model.FindMessageInTopicArguments;
import org.akhq.mcp.model.FindMessageInTopicResult;
import org.akhq.mcp.model.GetMessageDetailArguments;
import org.akhq.mcp.model.GetMessageDetailResult;
import org.akhq.configs.security.Role;
import org.akhq.mcp.services.AkhqTopicDataToolService;
import org.akhq.security.annotation.AKHQSecured;

import java.util.concurrent.ExecutionException;

@Secured(SecurityRule.IS_AUTHENTICATED)
@AKHQSecured(resource = Role.Resource.TOPIC_DATA, action = Role.Action.READ)
@Singleton
public class AkhqTools extends AbstractController {
    private final AkhqTopicDataToolService topicDataService;

    public AkhqTools(AkhqTopicDataToolService topicDataService) {
        this.topicDataService = topicDataService;
    }

    @Tool(
        name = "akhq.find_message_in_topic",
        description = """
            Search topic data and return matching message overviews.

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
              "maxMatches": 5
            }

            Rules:
            - `cluster` and `topic` are required.
            - Provide at least one of: `searchByKey`, `searchByValue`, `searchByHeaderKey`, `searchByHeaderValue`.
            - `timestamp` and `endTimestamp` accept ISO-8601 timestamps.
            - `maxMatches` defaults to 1 and is capped at 25.

            When presenting search results to a user, include each match's `partition`, `offset`,
            `timestamp`, `key`, and `valueOverview`. Do not reduce a matching message to only its
            partition, offset, and timestamp. Use `akhq.get_message_detail` when the full value
            payload or headers are needed.
            """
    )
    public FindMessageInTopicResult findMessageInTopic(FindMessageInTopicArguments arguments, MicronautMcpTransportContext transportContext)
        throws ExecutionException, InterruptedException {
        ensureTransportContext(transportContext);
        if (arguments == null) {
            throw new IllegalArgumentException("`arguments` is required");
        }

        String cluster = asRequiredString(arguments.cluster(), "`arguments.cluster` is required");
        String topicName = asRequiredString(arguments.topic(), "`arguments.topic` is required");
        checkIfClusterAndResourceAllowed(cluster, topicName);
        return topicDataService.findMessageInTopic(arguments);
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
    public GetMessageDetailResult getMessageDetail(GetMessageDetailArguments arguments, MicronautMcpTransportContext transportContext)
        throws ExecutionException, InterruptedException {
        ensureTransportContext(transportContext);
        if (arguments == null) {
            throw new IllegalArgumentException("`arguments` is required");
        }

        String cluster = asRequiredString(arguments.cluster(), "`arguments.cluster` is required");
        String topicName = asRequiredString(arguments.topic(), "`arguments.topic` is required");
        checkIfClusterAndResourceAllowed(cluster, topicName);
        return topicDataService.getMessageDetail(arguments);
    }

    private void ensureTransportContext(MicronautMcpTransportContext transportContext) {
        if (transportContext == null) {
            throw new IllegalArgumentException("MCP transport context is required");
        }
    }

    private String asRequiredString(String value, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }
}
