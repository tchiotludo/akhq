package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record TimeWindowSuggestion(
    String hint,
    String timestamp,
    String endTimestamp
) {
}

