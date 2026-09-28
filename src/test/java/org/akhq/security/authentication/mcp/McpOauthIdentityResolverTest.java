package org.akhq.security.authentication.mcp;

import org.akhq.configs.security.McpOauth;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpOauthIdentityResolverTest {
    @Test
    void resolvesNestedClaimsAndSingleValueGroups() {
        McpOauth configuration = new McpOauth();
        configuration.setUsernameClaim("preferred_username");
        configuration.setGroupsClaim("realm_access.roles");
        McpOauthIdentityResolver resolver = new McpOauthIdentityResolver(configuration);

        Map<String, Object> claims = Map.of(
            "preferred_username", "alice",
            "realm_access", Map.of("roles", "akhq-topic-readers")
        );

        assertEquals("alice", resolver.username(claims, "subject-123"));
        assertEquals(List.of("akhq-topic-readers"), resolver.groups(claims));
    }

    @Test
    void resolvesNamespacedClaimContainingDots() {
        McpOauth configuration = new McpOauth();
        configuration.setUsernameClaim("https://acme.com/email");
        configuration.setGroupsClaim("https://acme.com/groups");
        McpOauthIdentityResolver resolver = new McpOauthIdentityResolver(configuration);

        Map<String, Object> claims = Map.of(
            "https://acme.com/email", "alice@acme.com",
            "https://acme.com/groups", List.of("topic-readers", "operators")
        );

        assertEquals("alice@acme.com", resolver.username(claims, "subject-123"));
        assertEquals(List.of("topic-readers", "operators"), resolver.groups(claims));
    }

    @Test
    void fallsBackToSubjectAndNoGroupWhenClaimsAreMissing() {
        McpOauth configuration = new McpOauth();
        McpOauthIdentityResolver resolver = new McpOauthIdentityResolver(configuration);

        assertEquals("subject-123", resolver.username(Map.of(), "subject-123"));
        assertEquals(List.of(), resolver.groups(Map.of()));
    }
}
