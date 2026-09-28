package org.akhq.security.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.security.authentication.Authentication;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.security.Group;
import org.akhq.models.security.ClaimProvider;
import org.akhq.models.security.ClaimProviderType;
import org.akhq.models.security.ClaimRequest;
import org.akhq.security.authentication.mcp.McpOauthAuthentication;
import org.akhq.security.authentication.mcp.McpOauthIdentityResolver;
import org.akhq.security.rule.AKHQSecurityRule;

import java.util.Collection;
import java.util.List;

/**
 * Resolves the AKHQ groups of an authenticated user, whatever the authentication mechanism it comes from.
 * <p>
 * MCP OAuth authentications carry the identity provider claims, while the other authentication mechanisms rely on
 * the AKHQ issued token holding already mapped groups.
 */
@Singleton
@Slf4j
public class UserGroupsResolver {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ClaimProvider claimProvider;
    private final McpOauthIdentityResolver identityResolver;

    public UserGroupsResolver(ClaimProvider claimProvider, McpOauthIdentityResolver identityResolver) {
        this.claimProvider = claimProvider;
        this.identityResolver = identityResolver;
    }

    public List<Group> resolve(Authentication authentication) {
        if (authentication instanceof McpOauthAuthentication mcpOauthAuthentication) {
            return mcpOauthGroups(mcpOauthAuthentication);
        }

        return AKHQSecurityRule.unrollGroups(authentication, claimProvider).values().stream()
            .flatMap(Collection::stream)
            .map(group -> MAPPER.convertValue(group, Group.class))
            .toList();
    }

    private List<Group> mcpOauthGroups(McpOauthAuthentication authentication) {
        ClaimRequest request = ClaimRequest.builder()
            .providerType(ClaimProviderType.MCP_OAUTH)
            .username(identityResolver.username(authentication.getAttributes(), authentication.getName()))
            .groups(identityResolver.groups(authentication.getAttributes()))
            .build();

        try {
            return claimProvider.generateClaim(request).getGroups().values().stream()
                .flatMap(Collection::stream)
                .toList();
        } catch (Exception e) {
            // Fail closed: the caller ends up with the default group only. A custom ClaimProvider that does not
            // handle ClaimProviderType.MCP_OAUTH is the usual cause, so log it loudly enough to be actionable.
            log.error(
                "Unable to map the MCP OAuth claims of user '{}' to AKHQ groups, no group is granted. Check that the "
                    + "ClaimProvider in use handles ClaimProviderType.MCP_OAUTH.",
                request.getUsername(),
                e
            );
            return List.of();
        }
    }
}
