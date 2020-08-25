package org.akhq.modules;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.oauth2.configuration.OpenIdAdditionalClaimsConfiguration;
import io.micronaut.security.oauth2.endpoint.token.response.*;
import org.akhq.configs.Oidc;
import org.akhq.utils.UserGroupUtils;

import javax.annotation.Nonnull;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An OpenID user details mapper that is configurable in the akhq config.
 *
 * It will read a username and roles from the OpenID claims and translate them to akhq roles.
 */
@Singleton
@Replaces(DefaultOpenIdUserDetailsMapper.class)
@Requires(property = "akhq.security.oidc.enabled", value = StringUtils.TRUE)
public class OidcUserDetailsMapper implements OpenIdUserDetailsMapper {
    private final OpenIdAdditionalClaimsConfiguration openIdAdditionalClaimsConfiguration;
    private final UserGroupUtils userGroupUtils;
    private final Oidc oidc;

    /**
     * Default constructor.
     *
     * @param openIdAdditionalClaimsConfiguration The additional claims configuration
     * @param oidc the OIDC configuration
     * @param userGroupUtils the utils class to translate user groups
     */
    public OidcUserDetailsMapper(
            OpenIdAdditionalClaimsConfiguration openIdAdditionalClaimsConfiguration,
            Oidc oidc,
            UserGroupUtils userGroupUtils
    ) {
        this.openIdAdditionalClaimsConfiguration = openIdAdditionalClaimsConfiguration;
        this.oidc = oidc;
        this.userGroupUtils = userGroupUtils;
    }
    /**
     * Tries to read the username from the configured username field.
     *
     * @param providerName The OpenID provider name
     * @param tokenResponse The token response
     * @param openIdClaims The OpenID claims
     * @return The username to set in the {@link UserDetails}
     */
    protected String getUsername(String providerName, OpenIdTokenResponse tokenResponse, OpenIdClaims openIdClaims) {
        Oidc.Provider provider = oidc.getProvider(providerName);
        return Objects.toString(openIdClaims.get(provider.getUsernameField()));
    }

    /**
     * Tries to read groups from the configured groups field and translates them using {@link UserGroupUtils}.
     *
     * @param providerName The OpenID provider name
     * @param openIdClaims The OpenID claims
     * @param username The username used for mapping
     * @return The AKHQ internal groups
     */
    protected List<String> getAkhqGroups(String providerName, OpenIdClaims openIdClaims, String username) {
        Oidc.Provider provider = oidc.getProvider(providerName);
        Set<String> providerGroups = getOidcGroups(provider, openIdClaims);
        return UserGroupUtils.mapToAkhqGroups(username, providerGroups, provider.getGroups(), provider.getUsers(), provider.getDefaultGroup());
    }
    /**
     * Adds the configured attributes for the user roles to the user attributes.
     *
     * @param providerName The OpenID provider name
     * @param tokenResponse The token response
     * @param openIdClaims The OpenID claims
     * @return The attributes to set in the {@link UserDetails}
     */
    protected Map<String, Object> buildAttributes(
            String providerName,
            OpenIdTokenResponse tokenResponse,
            OpenIdClaims openIdClaims,
            List<String> akhqGroups
    ) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(OauthUserDetailsMapper.PROVIDER_KEY, providerName);
        if (openIdAdditionalClaimsConfiguration.isJwt()) {
            attributes.put(OpenIdUserDetailsMapper.OPENID_TOKEN_KEY, tokenResponse.getIdToken());
        }
        if (openIdAdditionalClaimsConfiguration.isAccessToken()) {
            attributes.put(OauthUserDetailsMapper.ACCESS_TOKEN_KEY, tokenResponse.getAccessToken());
        }
        if (openIdAdditionalClaimsConfiguration.isRefreshToken() && tokenResponse.getRefreshToken() != null) {
            attributes.put(OauthUserDetailsMapper.REFRESH_TOKEN_KEY, tokenResponse.getRefreshToken());
        }
        userGroupUtils.getUserAttributes(akhqGroups).forEach(attributes::put);
        return attributes;
    }

    /**
     * Tries to read groups from the configured groups field.
     * If the configured field cannot be found or isn't some kind of collection, it will return an empty set.
     *
     * @param provider The OpenID provider configuration
     * @param openIdClaims The OpenID claims
     * @return The groups from oidc
     */
    protected Set<String> getOidcGroups(Oidc.Provider provider, OpenIdClaims openIdClaims) {
        Set<String> groups = new HashSet<>();
        if (openIdClaims.contains(provider.getGroupsField())) {
            Object groupsField = openIdClaims.get(provider.getGroupsField());
            if (groupsField instanceof Collection) {
                groups = ((Collection<Object>) groupsField)
                        .stream()
                        .map(Objects::toString)
                        .collect(Collectors.toSet());
            }
        }
        return groups;
    }

    /**
     * @param providerName  The OpenID provider name
     * @param tokenResponse The token response
     * @param openIdClaims  The OpenID claims
     * @return A user details object
     */
    @Nonnull
    @Override
    public UserDetails createUserDetails(String providerName, OpenIdTokenResponse tokenResponse, OpenIdClaims openIdClaims) {
        String username = getUsername(providerName, tokenResponse, openIdClaims);
        List<String> akhqGroups = getAkhqGroups(providerName, openIdClaims, username);
        List<String> roles = userGroupUtils.getUserRoles(akhqGroups);
        Map<String, Object> attributes = buildAttributes(providerName, tokenResponse, openIdClaims, akhqGroups);
        return new UserDetails(username, roles, attributes);
    }
}
