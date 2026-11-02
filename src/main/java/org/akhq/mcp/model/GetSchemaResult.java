package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record GetSchemaResult(
    String cluster,
    String subject,
    Integer id,
    Integer version,
    List<Integer> versions,
    String schemaType,
    String compatibilityLevel,
    String schema,
    boolean schemaTruncated,
    List<Reference> references,
    String error
) {
    @Introspected
    public record Reference(String name, String subject, Integer version) {
    }
}
