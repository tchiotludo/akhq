package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record SearchSchemasResult(
    String cluster,
    int totalMatches,
    boolean truncated,
    List<String> subjects,
    String message
) {
}
