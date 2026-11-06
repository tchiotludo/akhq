package org.akhq.mcp.model;

/**
 * Arguments of an MCP tool operating on a single topic of a single cluster.
 */
public interface TopicScopedArguments extends ClusterScopedArguments {
    String topic();
}
