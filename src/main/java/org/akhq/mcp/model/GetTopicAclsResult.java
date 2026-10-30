package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record GetTopicAclsResult(
    String cluster,
    String topic,
    int totalMatches,
    boolean truncated,
    List<PrincipalAcls> principals,
    String message
) {
    @Introspected
    public record PrincipalAcls(String principal, List<GetPrincipalAclsResult.Acl> acls) {
    }
}
