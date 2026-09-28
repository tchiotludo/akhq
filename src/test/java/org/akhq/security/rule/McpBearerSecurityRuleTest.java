package org.akhq.security.rule;

import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.ServerAuthentication;
import io.micronaut.security.rules.SecurityRuleResult;
import org.akhq.configs.security.McpOauth;
import org.akhq.mcp.McpEndpoint;
import org.akhq.security.authentication.mcp.McpOauthAuthentication;
import org.akhq.security.authentication.mcp.McpOauthRequestMatcher;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpBearerSecurityRuleTest {
    @Test
    void ignoresRequestsOutsideTheMcpEndpoint() {
        assertEquals(
            SecurityRuleResult.UNKNOWN,
            check(rule(null), HttpRequest.GET("/api/topics"), mcpAuthentication(Map.of()))
        );
    }

    @Test
    void allowsValidatedMcpOauthAuthentication() {
        assertEquals(
            SecurityRuleResult.UNKNOWN,
            check(rule(null), HttpRequest.POST("/mcp", ""), mcpAuthentication(Map.of()))
        );
    }

    @Test
    void rejectsAnonymousRequests() {
        assertEquals(
            SecurityRuleResult.REJECTED,
            check(rule(null), HttpRequest.POST("/mcp", ""), null)
        );
    }

    @Test
    void rejectsAkhqUiAuthenticationReplayedOnTheMcpEndpoint() {
        Authentication uiAuthentication = new ServerAuthentication("einstein", List.of(), Map.of());

        assertEquals(
            SecurityRuleResult.REJECTED,
            check(rule(null), HttpRequest.POST("/mcp", ""), uiAuthentication)
        );
    }

    @Test
    void rejectsMcpOauthAuthenticationMissingTheRequiredScope() {
        assertEquals(
            SecurityRuleResult.REJECTED,
            check(rule("akhq.mcp.read"), HttpRequest.POST("/mcp", ""), mcpAuthentication(Map.of("scope", "openid profile")))
        );
    }

    @Test
    void allowsRequiredScopeFromSpaceDelimitedAndListClaims() {
        assertEquals(
            SecurityRuleResult.UNKNOWN,
            check(rule("akhq.mcp.read"), HttpRequest.POST("/mcp", ""), mcpAuthentication(Map.of("scope", "openid akhq.mcp.read")))
        );
        assertEquals(
            SecurityRuleResult.UNKNOWN,
            check(rule("akhq.mcp.read"), HttpRequest.POST("/mcp", ""), mcpAuthentication(Map.of("scope", List.of("akhq.mcp.read"))))
        );
    }

    private SecurityRuleResult check(McpBearerSecurityRule rule, HttpRequest<?> request, Authentication authentication) {
        return Mono.from(rule.check(request, authentication)).block();
    }

    private McpBearerSecurityRule rule(String requiredScope) {
        McpOauth configuration = new McpOauth();
        configuration.setEnabled(true);
        configuration.setRequiredScope(requiredScope);
        return new McpBearerSecurityRule(null, configuration, new McpOauthRequestMatcher(configuration, new McpEndpoint("", "/mcp")));
    }

    private Authentication mcpAuthentication(Map<String, Object> attributes) {
        return new McpOauthAuthentication("alice", attributes);
    }
}
