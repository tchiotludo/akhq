package org.akhq.security.mapper;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.StringUtils;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.oauth2.endpoint.authorization.state.State;
import io.micronaut.security.oauth2.endpoint.token.response.OauthAuthenticationMapper;
import io.micronaut.security.oauth2.endpoint.token.response.TokenResponse;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Named;
import org.akhq.configs.security.Oauth;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.akhq.models.security.GithubClaims;
import org.akhq.models.security.ClaimProvider;
import org.akhq.models.security.ClaimProviderType;
import org.akhq.models.security.ClaimRequest;
import org.akhq.models.security.ClaimResponse;
import org.akhq.security.authentication.GithubApiClient;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
@Named("github")
@Requires(property = "akhq.security.oauth2.enabled", value = StringUtils.TRUE)
public class GithubAuthenticationMapper implements OauthAuthenticationMapper {
    @Inject
    private Oauth oauth;
    @Inject
    private GithubApiClient apiClient;
    @Inject
    private ClaimProvider claimProvider;

    @Override
    public Publisher<AuthenticationResponse> createAuthenticationResponse(TokenResponse tokenResponse, @Nullable State state) {
        return Flux.from(apiClient.getUser("token " + tokenResponse.getAccessToken()))
            .map(user -> {
                ClaimRequest request = ClaimRequest.builder()
                    .providerType(ClaimProviderType.OAUTH)
                    .providerName("github")
                    .username(getUsername(oauth.getProvider("github"), user))
                    .groups(getOauthGroups(oauth.getProvider("github"), user))
                    .build();

                ClaimResponse claim = claimProvider.generateClaim(request);

                return AuthenticationResponse.success(getUsername(oauth.getProvider("github"), user), List.of(SecurityRule.IS_AUTHENTICATED), Map.of("groups", claim.getGroups()));
            });
    }

    /**
     * Tries to read the username from the configured username field.
     *
     * @param provider  The OAuth provider
     * @param user  The OAuth claims
     * @return The username to set in the {@link io.micronaut.security.authentication.Authentication}
     */
    protected String getUsername(Oauth.Provider provider, GithubClaims user) {
        String userNameField = provider.getUsernameField();
        return Objects.toString(user.get(userNameField));
    }

    /**
     * Tries to read groups from the configured groups field.
     * If the configured field cannot be found or isn't some kind of collection, it will return an empty set.
     *
     * @param provider     The OAuth provider configuration
     * @param user The OAuth claims
     * @return The groups from oauth
     */
    protected List<String> getOauthGroups(Oauth.Provider provider, GithubClaims user) {
        List<String> groups = new ArrayList<>();
        if (user.contains(provider.getGroupsField())) {
            Object groupsField = user.get(provider.getGroupsField());
            if (groupsField instanceof Collection) {
                groups = ((Collection<Object>) groupsField)
                    .stream()
                    .map(Objects::toString)
                    .collect(Collectors.toList());
            } else if (groupsField instanceof String) {
                groups.add((String) groupsField);
            }
        }
        return groups;
    }
}