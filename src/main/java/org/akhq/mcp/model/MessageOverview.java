package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record MessageOverview(
    int partition,
    long offset,
    String timestamp,
    String key,
    String valueOverview
) {
}

