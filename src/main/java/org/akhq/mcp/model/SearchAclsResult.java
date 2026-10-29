package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record SearchAclsResult(
    String cluster,
    int totalMatches,
    boolean truncated,
    List<PrincipalOverview> principals,
    String message
) {
    @Introspected
    public record PrincipalOverview(String principal, int aclCount) {
    }
}
