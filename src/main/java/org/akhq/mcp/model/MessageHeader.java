package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record MessageHeader(
    String key,
    String value
) {
}

