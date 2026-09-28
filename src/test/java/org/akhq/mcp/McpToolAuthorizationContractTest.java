package org.akhq.mcp;

import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import org.akhq.mcp.model.ClusterScopedArguments;
import org.akhq.mcp.model.TopicScopedArguments;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MCP tools are dispatched by the MCP server instead of the HTTP router, so their authorization relies on the
 * guard of {@link AbstractMcpTool} rather than on the security filters. These tests keep that contract enforced
 * when new tools are added.
 */
class McpToolAuthorizationContractTest {
    @Test
    void everyToolHolderExtendsTheGuardedBaseClass() {
        assertTrue(
            AbstractMcpTool.class.isAssignableFrom(AkhqTools.class),
            "AkhqTools must extend AbstractMcpTool to inherit the authorization guard"
        );
    }

    @Test
    void everyToolTakesScopedArguments() {
        List<Method> tools = Arrays.stream(AkhqTools.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(Tool.class))
            .toList();

        assertFalse(tools.isEmpty(), "No @Tool method found, the reflection lookup is broken");

        tools.forEach(tool -> assertTrue(
            Arrays.stream(tool.getParameterTypes()).anyMatch(ClusterScopedArguments.class::isAssignableFrom),
            "Tool '" + tool.getName() + "' must take ClusterScopedArguments so it can be authorized by "
                + "AbstractMcpTool.authorizeClusterScope or AbstractMcpTool.authorizeTopicScope"
        ));
    }

    @Test
    void theGuardIsDeclaredOutsideTheToolClass() throws NoSuchMethodException {
        // AbstractController resolves @AKHQSecured by walking the stack up to the first frame declared by the
        // concrete tool class. If the guard were declared in AkhqTools, the walker would stop on the guard and a
        // per-method @AKHQSecured annotation would be silently ignored.
        List<Method> guards = List.of(
            AbstractMcpTool.class.getDeclaredMethod(
                "authorizeTopicScope", TopicScopedArguments.class, MicronautMcpTransportContext.class
            ),
            AbstractMcpTool.class.getDeclaredMethod(
                "authorizeClusterScope", ClusterScopedArguments.class, MicronautMcpTransportContext.class
            )
        );

        guards.forEach(guard -> assertNotEquals(
            AkhqTools.class,
            guard.getDeclaringClass(),
            "The authorization guard '" + guard.getName() + "' must stay declared in AbstractMcpTool"
        ));
    }
}
