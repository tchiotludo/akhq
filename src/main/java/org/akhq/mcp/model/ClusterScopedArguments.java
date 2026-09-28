package org.akhq.mcp.model;

/**
 * Arguments of an MCP tool operating on a single cluster.
 * <p>
 * Implementing this interface is what makes a tool eligible to the shared authorization guard of
 * {@code AbstractMcpTool}, so every new tool goes through the same permission check.
 */
public interface ClusterScopedArguments {
    String cluster();
}
