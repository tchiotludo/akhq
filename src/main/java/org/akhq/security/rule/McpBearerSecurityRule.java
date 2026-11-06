package org.akhq.security.rule;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.AbstractSecurityRule;
import io.micronaut.security.rules.SecuredAnnotationRule;
import io.micronaut.security.rules.SecurityRuleResult;
import io.micronaut.security.token.RolesFinder;
import jakarta.inject.Singleton;
import org.akhq.configs.security.McpOauth;
import org.akhq.security.authentication.mcp.McpOauthAuthentication;
import org.akhq.security.authentication.mcp.McpOauthRequestMatcher;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Singleton
@Requires(property = "akhq.security.mcp-oauth.enabled", value = "true")
public class McpBearerSecurityRule extends AbstractSecurityRule<HttpRequest<?>> {
    private final McpOauth mcpOauth;
    private final McpOauthRequestMatcher requestMatcher;

    public McpBearerSecurityRule(RolesFinder rolesFinder, McpOauth mcpOauth, McpOauthRequestMatcher requestMatcher) {
        super(rolesFinder);
        this.mcpOauth = mcpOauth;
        this.requestMatcher = requestMatcher;
    }

    @Override
    public Publisher<SecurityRuleResult> check(HttpRequest<?> request, Authentication authentication) {
        if (!requestMatcher.matches(request)) {
            return Mono.just(SecurityRuleResult.UNKNOWN);
        }

        // Only an identity built from a validated MCP OAuth access token may reach the MCP endpoint. Any other
        // authentication, such as an AKHQ issued UI token replayed as a bearer token, is rejected.
        if (!(authentication instanceof McpOauthAuthentication) || !hasRequiredScope(authentication)) {
            return Mono.just(SecurityRuleResult.REJECTED);
        }
        return Mono.just(SecurityRuleResult.UNKNOWN);
    }

    @Override
    public int getOrder() {
        return SecuredAnnotationRule.ORDER - 200;
    }

    private boolean hasRequiredScope(Authentication authentication) {
        String requiredScope = mcpOauth.getRequiredScope();
        if (requiredScope == null || requiredScope.isBlank()) {
            return true;
        }

        Object scopes = authentication.getAttributes().get("scope");
        if (scopes instanceof String scope) {
            return java.util.Arrays.asList(scope.split("\\s+")).contains(requiredScope);
        }
        if (scopes instanceof Collection<?> scopeList) {
            return scopeList.stream().map(String::valueOf).anyMatch(requiredScope::equals);
        }
        return false;
    }
}
