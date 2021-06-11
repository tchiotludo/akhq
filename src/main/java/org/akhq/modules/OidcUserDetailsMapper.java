package org.akhq.modules;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.StringUtils;
import io.micronaut.security.authentication.AuthenticationFailed;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.config.AuthenticationModeConfiguration;
import io.micronaut.security.oauth2.configuration.OpenIdAdditionalClaimsConfiguration;
import io.micronaut.security.oauth2.endpoint.authorization.state.State;
import io.micronaut.security.oauth2.endpoint.token.response.DefaultOpenIdUserDetailsMapper;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdTokenResponse;
import org.akhq.configs.Oidc;
import org.akhq.utils.ClaimProvider;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An OpenID user details mapper that is configurable in the akhq config.
 * <p>
 * It will read a username and roles from the OpenID claims and translate them to akhq roles.
 */
@Singleton
@Replaces(DefaultOpenIdUserDetailsMapper.class)
@Requires(property = "akhq.security.oidc.enabled", value = StringUtils.TRUE)
public class OidcUserDetailsMapper extends DefaultOpenIdUserDetailsMapper {
    @Inject
    private Oidc oidc;
    @Inject
    private ClaimProvider claimProvider;

    public OidcUserDetailsMapper(OpenIdAdditionalClaimsConfiguration openIdAdditionalClaimsConfiguration, AuthenticationModeConfiguration authenticationModeConfiguration) {
        super(openIdAdditionalClaimsConfiguration, authenticationModeConfiguration);
    }

    @NonNull
    @Override
    public AuthenticationResponse createAuthenticationResponse(String providerName, OpenIdTokenResponse tokenResponse, OpenIdClaims openIdClaims, @Nullable State state) {
        // get username and groups declared from OIDC system
        String oidcUsername = getUsername(providerName, tokenResponse, openIdClaims);
        List<String> oidcGroups = getOidcGroups(oidc.getProvider(providerName), openIdClaims);

        ClaimProvider.AKHQClaimRequest request = ClaimProvider.AKHQClaimRequest.builder()
                .providerType(ClaimProvider.ProviderType.OIDC)
                .providerName(providerName)
                .username(oidcUsername)
                .groups(oidcGroups)
                .build();

        try {
            ClaimProvider.AKHQClaimResponse claim = claimProvider.generateClaim(request);
            return new UserDetails(oidcUsername, claim.getRoles(), claim.getAttributes());
        } catch (Exception e) {
            String claimProviderClass = claimProvider.getClass().getName();
            return new AuthenticationFailed("Exception from ClaimProvider " + claimProviderClass + ": " + e.getMessage());
        }
    }

    /**
     * Tries to read the username from the configured username field.
     *
     * @param providerName  The OpenID provider name
     * @param tokenResponse The token response
     * @param openIdClaims  The OpenID claims
     * @return The username to set in the {@link UserDetails}
     */
    protected String getUsername(String providerName, OpenIdTokenResponse tokenResponse, OpenIdClaims openIdClaims) {
        Oidc.Provider provider = oidc.getProvider(providerName);
        return Objects.toString(openIdClaims.get(provider.getUsernameField()));
    }

    /**
     * Tries to read groups from the configured groups field.
     * If the configured field cannot be found or isn't some kind of collection, it will return an empty set.
     *
     * @param provider     The OpenID provider configuration
     * @param openIdClaims The OpenID claims
     * @return The groups from oidc
     */
    protected List<String> getOidcGroups(Oidc.Provider provider, OpenIdClaims openIdClaims) {
        List<String> groups = new ArrayList<>();
        if (openIdClaims.contains(provider.getGroupsField())) {
            Object groupsField = openIdClaims.get(provider.getGroupsField());
            if (groupsField instanceof Collection) {
                groups = ((Collection<Object>) groupsField)
                        .stream()
                        .map(Objects::toString)
                        .collect(Collectors.toList());
            }
        }
        return groups;
    }
}
