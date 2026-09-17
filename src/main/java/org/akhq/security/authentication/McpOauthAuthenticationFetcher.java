package org.akhq.security.authentication;

import com.nimbusds.jwt.JWTClaimsSet;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.ServerAuthentication;
import io.micronaut.security.filters.AuthenticationFetcher;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.security.McpOauth;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Singleton
@Requires(property = "akhq.security.mcp-oauth.enabled", value = "true")
public class McpOauthAuthenticationFetcher implements AuthenticationFetcher<HttpRequest<?>> {
    public static final String AUTHENTICATION_ATTRIBUTE = "akhq.mcp-oauth.authentication";

    private final McpOauth mcpOauth;
    private final McpOauthTokenValidator tokenValidator;
    private final McpOauthIdentityResolver identityResolver;
    private final McpOauthRequestMatcher requestMatcher;

    public McpOauthAuthenticationFetcher(
        McpOauth mcpOauth,
        McpOauthTokenValidator tokenValidator,
        McpOauthIdentityResolver identityResolver,
        McpOauthRequestMatcher requestMatcher
    ) {
        this.mcpOauth = mcpOauth;
        this.tokenValidator = tokenValidator;
        this.identityResolver = identityResolver;
        this.requestMatcher = requestMatcher;
    }

    @Override
    public Publisher<Authentication> fetchAuthentication(HttpRequest<?> request) {
        if (!requestMatcher.matches(request)) {
            return Publishers.empty();
        }

        Optional<String> token = request.getHeaders().getAuthorization()
            .filter(value -> value.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length()))
            .map(value -> value.substring("Bearer ".length()).trim())
            .filter(value -> !value.isEmpty());
        if (token.isEmpty()) {
            return Publishers.empty();
        }

        return Mono.fromCallable(() -> authenticate(token.get()))
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume(exception -> {
                log.debug("Rejected MCP OAuth access token: {}", exception.getMessage());
                return Mono.empty();
            });
    }

    private Authentication authenticate(String token) throws Exception {
        JWTClaimsSet claims = tokenValidator.validate(token);
        String username = identityResolver.username(claims);
        Map<String, Object> attributes = new LinkedHashMap<>(claims.getClaims());
        attributes.put(AUTHENTICATION_ATTRIBUTE, true);
        return new ServerAuthentication(username, List.of(SecurityRule.IS_AUTHENTICATED), attributes);
    }
}
