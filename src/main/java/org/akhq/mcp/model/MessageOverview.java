package org.akhq.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Introspected;

import java.util.Map;

@Introspected
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageOverview(
    int partition,
    long offset,
    String timestamp,
    String key,
    String value,
    Boolean valueTruncated,
    Map<String, Object> fields
) {
}
