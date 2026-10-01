package org.akhq.mcp;

import io.micronaut.json.JsonMapper;
import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import io.micronaut.security.authentication.AuthorizationException;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import jakarta.inject.Inject;
import org.akhq.controllers.AbstractController;
import org.akhq.mcp.model.ClusterScopedArguments;
import org.akhq.mcp.model.TopicScopedArguments;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Base class of the MCP tool holders.
 * <p>
 * MCP tools are dispatched by the MCP server, not by the HTTP router, so the {@code @Secured} and
 * {@code @AKHQSecured} annotations of a tool class are not enforced by the HTTP security filters the way they are
 * for a regular controller. Every tool must therefore run the AKHQ permission check itself.
 * {@link #authorizeTopicScope(TopicScopedArguments, MicronautMcpTransportContext)} and
 * {@link #authorizeClusterScope(ClusterScopedArguments, MicronautMcpTransportContext)} make that check a single
 * mandatory call that also yields the validated arguments, so a tool cannot read Kafka data without it.
 * <p>
 * The guard lives in this class on purpose: {@code AbstractController} resolves the {@code @AKHQSecured} annotation
 * by walking the stack up to the first frame declared by the concrete tool class. Keeping the guard in a superclass
 * means the walker still lands on the tool method itself, so a per-method {@code @AKHQSecured} annotation keeps
 * taking precedence over the class-level one. For the same reason, a tool must call the guard directly from its
 * method body, not from a lambda.
 * <p>
 * Tools return a {@link CallToolResult}: invalid arguments and missing permissions are reported with
 * {@link #toolError(RuntimeException)} as tool execution errors ({@code isError: true}), as required by the MCP
 * specification, so the language model gets the reason and can correct its call. Only unexpected failures end up
 * as JSON-RPC errors.
 */
abstract class AbstractMcpTool extends AbstractController {
    static final String FORBIDDEN_MESSAGE = "Forbidden: you are not allowed to access this cluster or resource";

    @Inject
    private JsonMapper jsonMapper;

    /**
     * Validates the common arguments of a topic scoped tool and checks that the caller is allowed to access the
     * requested cluster and topic.
     *
     * @return the trimmed cluster and topic names
     * @throws IllegalArgumentException                              when the transport context or an argument is missing
     * @throws io.micronaut.security.authentication.AuthorizationException when the caller lacks the permission
     */
    protected TopicScope authorizeTopicScope(TopicScopedArguments arguments, MicronautMcpTransportContext transportContext) {
        String cluster = requiredCluster(arguments, transportContext);
        String topic = required(arguments.topic(), "`arguments.topic` is required");
        checkIfClusterAndResourceAllowed(cluster, topic);

        return new TopicScope(cluster, topic);
    }

    /**
     * Validates the common arguments of a cluster scoped tool, checks that the caller is allowed to access the
     * requested cluster, and resolves the resource name patterns the caller is restricted to.
     *
     * @return the trimmed cluster name and the resource name patterns to apply. An empty list means no restriction.
     * @throws IllegalArgumentException                              when the transport context or an argument is missing
     * @throws io.micronaut.security.authentication.AuthorizationException when the caller lacks the permission
     */
    protected ClusterScope authorizeClusterScope(ClusterScopedArguments arguments, MicronautMcpTransportContext transportContext) {
        String cluster = requiredCluster(arguments, transportContext);
        checkIfClusterAllowed(cluster);

        return new ClusterScope(cluster, buildUserBasedResourceFilters(cluster));
    }

    private String requiredCluster(ClusterScopedArguments arguments, MicronautMcpTransportContext transportContext) {
        if (transportContext == null) {
            throw new IllegalArgumentException("MCP transport context is required");
        }
        if (arguments == null) {
            throw new IllegalArgumentException("`arguments` is required");
        }

        return required(arguments.cluster(), "`arguments.cluster` is required");
    }

    private String required(String value, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }

    /**
     * @return the result serialized as JSON text
     */
    protected CallToolResult toolResult(Object result) {
        try {
            return CallToolResult.builder()
                .addTextContent(jsonMapper.writeValueAsString(result))
                .isError(false)
                .build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @return a tool execution error for an invalid argument or a missing permission
     */
    protected CallToolResult toolError(RuntimeException exception) {
        String message = exception instanceof AuthorizationException ? FORBIDDEN_MESSAGE : exception.getMessage();
        return CallToolResult.builder()
            .addTextContent(message)
            .isError(true)
            .build();
    }

    protected record TopicScope(String cluster, String topic) {
    }

    protected record ClusterScope(String cluster, List<String> resourceFilters) {
    }
}
