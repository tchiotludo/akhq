package org.akhq.mcp;

import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import org.akhq.controllers.AbstractController;
import org.akhq.mcp.model.TopicScopedArguments;

/**
 * Base class of the MCP tool holders.
 * <p>
 * MCP tools are dispatched by the MCP server, not by the HTTP router, so the {@code @Secured} and
 * {@code @AKHQSecured} annotations of a tool class are not enforced by the HTTP security filters the way they are
 * for a regular controller. Every tool must therefore run the AKHQ permission check itself.
 * {@link #authorizeTopicScope(TopicScopedArguments, MicronautMcpTransportContext)} makes that check a single
 * mandatory call that also yields the validated arguments, so a tool cannot read topic data without it.
 * <p>
 * The guard lives in this class on purpose: {@code AbstractController} resolves the {@code @AKHQSecured} annotation
 * by walking the stack up to the first frame declared by the concrete tool class. Keeping the guard in a superclass
 * means the walker still lands on the tool method itself, so a per-method {@code @AKHQSecured} annotation keeps
 * taking precedence over the class-level one.
 */
abstract class AbstractMcpTool extends AbstractController {
    /**
     * Validates the common arguments of a topic scoped tool and checks that the caller is allowed to access the
     * requested cluster and topic.
     *
     * @return the trimmed cluster and topic names
     * @throws IllegalArgumentException                              when the transport context or an argument is missing
     * @throws io.micronaut.security.authentication.AuthorizationException when the caller lacks the permission
     */
    protected TopicScope authorizeTopicScope(TopicScopedArguments arguments, MicronautMcpTransportContext transportContext) {
        if (transportContext == null) {
            throw new IllegalArgumentException("MCP transport context is required");
        }
        if (arguments == null) {
            throw new IllegalArgumentException("`arguments` is required");
        }

        String cluster = required(arguments.cluster(), "`arguments.cluster` is required");
        String topic = required(arguments.topic(), "`arguments.topic` is required");
        checkIfClusterAndResourceAllowed(cluster, topic);

        return new TopicScope(cluster, topic);
    }

    private String required(String value, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }

    protected record TopicScope(String cluster, String topic) {
    }
}
