package org.akhq.security.authentication;

import io.micronaut.security.authentication.Authentication;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.security.Group;
import org.akhq.models.security.ClaimProvider;
import org.akhq.models.security.ClaimProviderType;
import org.akhq.models.security.ClaimRequest;

import java.util.List;

@Singleton
@Slf4j
public class McpOauthGroupResolver {
    private final ClaimProvider claimProvider;
    private final McpOauthIdentityResolver identityResolver;

    public McpOauthGroupResolver(ClaimProvider claimProvider, McpOauthIdentityResolver identityResolver) {
        this.claimProvider = claimProvider;
        this.identityResolver = identityResolver;
    }

    public boolean isMcpOauthAuthentication(Authentication authentication) {
        return Boolean.TRUE.equals(authentication.getAttributes().get(McpOauthAuthenticationFetcher.AUTHENTICATION_ATTRIBUTE));
    }

    public List<Group> groups(Authentication authentication) {
        String username = identityResolver.username(authentication.getAttributes(), authentication.getName());
        ClaimRequest request = ClaimRequest.builder()
            .providerType(ClaimProviderType.MCP_OAUTH)
            .username(username)
            .groups(identityResolver.groups(authentication.getAttributes()))
            .build();

        try {
            return claimProvider.generateClaim(request).getGroups().values().stream()
                .flatMap(List::stream)
                .toList();
        } catch (Exception e) {
            log.warn("Unable to map MCP OAuth groups: {}", e.getMessage());
            return List.of();
        }
    }
}
