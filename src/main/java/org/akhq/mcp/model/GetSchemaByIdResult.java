package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record GetSchemaByIdResult(
    String cluster,
    int id,
    List<SubjectVersion> subjects,
    GetSchemaResult schema,
    String message
) {
    @Introspected
    public record SubjectVersion(String subject, Integer version) {
    }
}
