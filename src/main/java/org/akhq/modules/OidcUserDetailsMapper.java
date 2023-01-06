package org.akhq.modules;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.StringUtils;
import io.micronaut.security.authentication.AuthenticationFailed;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.config.AuthenticationModeConfiguration;
import io.micronaut.security.oauth2.configuration.OpenIdAdditionalClaimsConfiguration;
import io.micronaut.security.oauth2.endpoint.authorization.state.State;
import io.micronaut.security.oauth2.endpoint.token.response.DefaultOpenIdAuthenticationMapper;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdTokenResponse;
import org.akhq.configs.Oidc;
import org.akhq.utils.ClaimRequest;
import org.akhq.utils.ClaimResponse;
import org.akhq.utils.ClaimProvider;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.akhq.utils.ClaimProviderType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An OpenID user details mapper that is configurable in the akhq config.
 * <p>
 * It will read a username and roles from the OpenID claims and translate them to akhq roles.
 */
@Singleton
@Replaces(DefaultOpenIdAuthenticationMapper.class)
@Requires(property = "akhq.security.oidc.enabled", value = StringUtils.TRUE)
public class OidcUserDetailsMapper extends DefaultOpenIdAuthenticationMapper {
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
        // get the current OIDC provider
        Oidc.Provider provider = oidc.getProvider(providerName);

        // get username and groups declared from OIDC system
        String oidcUsername = getUsername(provider, openIdClaims);

        // Some OIDC providers like Keycloak can return a claim with roles and attributes directly,
        // so we don't use the AKHQ internal ClaimProvider mechanism
        if(provider.isUseOidcClaim()){
            return createDirectClaimAuthenticationResponse(oidcUsername, openIdClaims);
        }

        List<String> oidcGroups = getOidcGroups(provider, openIdClaims);

        ClaimRequest request = ClaimRequest.builder()
                .providerType(ClaimProviderType.OIDC)
                .providerName(providerName)
                .username(oidcUsername)
                .groups(oidcGroups)
                .build();

        try {
            ClaimResponse claim = claimProvider.generateClaim(request);
            return AuthenticationResponse.success(oidcUsername, claim.getRoles(), claim.getAttributes());
        } catch (Exception e) {
            String claimProviderClass = claimProvider.getClass().getName();
            return new AuthenticationFailed("Exception from ClaimProvider " + claimProviderClass + ": " + e.getMessage());
        }
    }

    private AuthenticationResponse createDirectClaimAuthenticationResponse(String oidcUsername, OpenIdClaims openIdClaims) {
        String ROLES_KEY = "roles";
        if(openIdClaims.contains(ROLES_KEY) && openIdClaims.get(ROLES_KEY) instanceof List){
            List<String> roles = (List<String>) openIdClaims.get(ROLES_KEY);
            Map<String, Object> attributes =  openIdClaims.getClaims()
                .entrySet()
                .stream()
                // keep only topicsFilterRegexp, connectsFilterRegexp, consumerGroupsFilterRegexp and potential future filters
                .filter(kv -> kv.getKey().matches(".*FilterRegexp$"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            return AuthenticationResponse.success(oidcUsername, roles, attributes);
        }

        return new AuthenticationFailed("Exception during Authentication: use-oidc-claim config requires attribute " +
            ROLES_KEY + " in the OIDC claim");
    }

    /**
     * Tries to read the username from the configured username field.
     *
     * @param provider  The OpenID provider
     * @param openIdClaims  The OpenID claims
     * @return The username to set in the {@link io.micronaut.security.authentication.Authentication}
     */
    protected String getUsername(Oidc.Provider provider, OpenIdClaims openIdClaims) {
        final Object username = getClaimValue(openIdClaims, provider.getUsernameField());
        return Objects.toString(username);
    }

    /**
     * Tries to read groups from the configured groups field.
     * If the configured field cannot be found or isn't some kind of collection or string, it will return an empty set.
     *
     * @param provider     The OpenID provider configuration
     * @param openIdClaims The OpenID claims
     * @return The groups from oidc
     */
    protected List<String> getOidcGroups(Oidc.Provider provider, OpenIdClaims openIdClaims) {
        List<String> groups = new ArrayList<>();
        Object groupsField = getClaimValue(openIdClaims, provider.getGroupsField());
        // When the user belongs to only one group, groupsField can either be an array (with one item)
        // or a string, depending on the IdP implementation.
        if (groupsField instanceof Collection) {
            groups = ((Collection<Object>) groupsField)
                    .stream()
                    .map(Objects::toString)
                    .collect(Collectors.toList());
        } else if (groupsField instanceof String) {
            groups.add((String) groupsField);
        }
        return groups;
    }

    private Object getClaimValue(OpenIdClaims openIdClaims, String name) {
        final String[] subFields = name.split("\\.");
        Object claimValue = openIdClaims.get(subFields[0]);
        for(int i = 1; i < subFields.length; i++) {
            final String subField = subFields[i];
            if (claimValue instanceof Map) {
                claimValue = ((Map) claimValue).get(subField);
            } else {
                break;
            }
        }
        return claimValue;
    }
}
