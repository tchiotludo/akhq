package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record GetPrincipalAclsResult(
    String cluster,
    String principal,
    int totalAcls,
    boolean truncated,
    List<Acl> acls,
    String message
) {
    @Introspected
    public record Acl(
        String resourceType,
        String resourceName,
        String patternType,
        String operation,
        String permissionType,
        String host
    ) {
    }
}
