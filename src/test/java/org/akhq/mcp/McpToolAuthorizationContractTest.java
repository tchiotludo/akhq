package org.akhq.mcp;

import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import org.akhq.mcp.model.ClusterScopedArguments;
import org.akhq.mcp.model.ListClustersArguments;
import org.akhq.security.annotation.AKHQSecured;
import org.akhq.mcp.model.TopicScopedArguments;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MCP tools are dispatched by the MCP server instead of the HTTP router, so their authorization relies on the
 * guard of {@link AbstractMcpTool} rather than on the security filters. These tests keep that contract enforced
 * when new tools are added.
 */
class McpToolAuthorizationContractTest {
    private static final List<Class<? extends AbstractMcpTool>> TOOL_HOLDERS = List.of(
        AkhqTools.class,
        AkhqClusterTools.class,
        AkhqConsumerGroupTools.class,
        AkhqSchemaTools.class,
        AkhqConnectTools.class,
        AkhqAclTools.class
    );

    @Test
    void everyToolHolderExtendsTheGuardedBaseClass() {
        TOOL_HOLDERS.forEach(holder -> assertTrue(
            AbstractMcpTool.class.isAssignableFrom(holder),
            holder.getSimpleName() + " must extend AbstractMcpTool to inherit the authorization guard"
        ));
    }

    @Test
    void everyToolHolderDeclaresTheRequiredPermission() {
        TOOL_HOLDERS.forEach(holder -> assertTrue(
            holder.isAnnotationPresent(AKHQSecured.class),
            holder.getSimpleName() + " must declare the @AKHQSecured permission its tools are checked against"
        ));
    }

    @Test
    void everyToolHolderIsListed() throws IOException {
        try (var sources = Files.list(Path.of("src/main/java/org/akhq/mcp"))) {
            List<String> declared = sources
                .map(path -> path.getFileName().toString())
                .filter(name -> name.startsWith("Akhq") && name.endsWith("Tools.java"))
                .map(name -> name.replace(".java", ""))
                .sorted()
                .toList();

            assertEquals(
                TOOL_HOLDERS.stream().map(Class::getSimpleName).sorted().toList(),
                declared,
                "New tool holders must be added to TOOL_HOLDERS so their authorization contract is checked"
            );
        }
    }

    @Test
    void everyToolTakesScopedArguments() {
        List<Method> tools = TOOL_HOLDERS.stream()
            .flatMap(holder -> Arrays.stream(holder.getDeclaredMethods()))
            .filter(method -> method.isAnnotationPresent(Tool.class))
            .toList();

        assertFalse(tools.isEmpty(), "No @Tool method found, the reflection lookup is broken");

        tools.stream()
            // akhq.list_clusters targets no cluster: the list is already limited to the caller's groups
            .filter(tool -> !Arrays.asList(tool.getParameterTypes()).contains(ListClustersArguments.class))
            .forEach(tool -> assertTrue(
                Arrays.stream(tool.getParameterTypes()).anyMatch(ClusterScopedArguments.class::isAssignableFrom),
                "Tool '" + tool.getName() + "' must take ClusterScopedArguments so it can be authorized by "
                    + "AbstractMcpTool.authorizeClusterScope, authorizeTopicScope or authorizeResourceScope"
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
            ),
            AbstractMcpTool.class.getDeclaredMethod(
                "authorizeResourceScope",
                ClusterScopedArguments.class, MicronautMcpTransportContext.class, String.class, String.class
            )
        );

        guards.forEach(guard -> assertFalse(
            TOOL_HOLDERS.contains(guard.getDeclaringClass()),
            "The authorization guard '" + guard.getName() + "' must stay declared in AbstractMcpTool"
        ));
    }
}
