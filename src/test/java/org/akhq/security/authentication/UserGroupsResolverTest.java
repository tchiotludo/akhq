package org.akhq.security.authentication;

import io.micronaut.security.authentication.ServerAuthentication;
import org.akhq.configs.security.Group;
import org.akhq.configs.security.McpOauth;
import org.akhq.models.security.ClaimProvider;
import org.akhq.models.security.ClaimProviderType;
import org.akhq.models.security.ClaimRequest;
import org.akhq.models.security.ClaimResponse;
import org.akhq.security.authentication.mcp.McpOauthAuthentication;
import org.akhq.security.authentication.mcp.McpOauthIdentityResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserGroupsResolverTest {
    private final McpOauth mcpOauth = new McpOauth();

    @Test
    void shouldMapMcpOauthClaimsToAkhqGroups() {
        ClaimRequest[] captured = new ClaimRequest[1];
        Group group = new Group();
        group.setRole("topic-reader");
        ClaimProvider claimProvider = request -> {
            captured[0] = request;
            return ClaimResponse.builder().groups(Map.of("topic-reader", List.of(group))).build();
        };

        UserGroupsResolver resolver = new UserGroupsResolver(claimProvider, new McpOauthIdentityResolver(mcpOauth));
        List<Group> groups = resolver.resolve(new McpOauthAuthentication(
            "subject-id",
            Map.of("preferred_username", "einstein", "groups", List.of("mcp-readers"))
        ));

        assertEquals(List.of(group), groups);
        assertEquals(ClaimProviderType.MCP_OAUTH, captured[0].getProviderType());
        assertEquals("einstein", captured[0].getUsername());
        assertEquals(List.of("mcp-readers"), captured[0].getGroups());
    }

    @Test
    void shouldReturnNoGroupWhenMcpOauthClaimMappingFails() {
        ClaimProvider claimProvider = request -> {
            throw new IllegalStateException("mapping failure");
        };

        UserGroupsResolver resolver = new UserGroupsResolver(claimProvider, new McpOauthIdentityResolver(mcpOauth));

        assertTrue(resolver.resolve(new McpOauthAuthentication("subject-id", Map.of())).isEmpty());
    }

    @Test
    void shouldUseAkhqTokenGroupsForNonMcpAuthentication() {
        ClaimProvider claimProvider = request -> ClaimResponse.builder().groups(Map.of()).build();
        UserGroupsResolver resolver = new UserGroupsResolver(claimProvider, new McpOauthIdentityResolver(mcpOauth));

        List<Group> groups = resolver.resolve(new ServerAuthentication(
            "einstein",
            List.of(),
            Map.of("groups", Map.of("topic-reader", List.of(Map.of("role", "topic-reader"))))
        ));

        assertEquals(1, groups.size());
        assertEquals("topic-reader", groups.getFirst().getRole());
    }

    @Test
    void shouldNotMapMcpClaimsForNonMcpAuthentication() {
        ClaimProvider claimProvider = request -> ClaimResponse.builder().groups(Map.of()).build();
        UserGroupsResolver resolver = new UserGroupsResolver(claimProvider, new McpOauthIdentityResolver(mcpOauth));

        // A raw identity provider group list is not the AKHQ token format and must not be silently accepted.
        assertThrows(RuntimeException.class, () -> resolver.resolve(new ServerAuthentication(
            "einstein",
            List.of(),
            Map.of("groups", List.of("mcp-readers"))
        )));
    }
}
