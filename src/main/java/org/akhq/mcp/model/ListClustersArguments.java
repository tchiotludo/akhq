package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.jsonschema.JsonSchema;

/**
 * Arguments of the tools that do not target a cluster.
 */
@JsonSchema
@Introspected
public record ListClustersArguments() {
}
