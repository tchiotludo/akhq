package org.akhq.modules;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.StringUtils;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.config.AuthenticationModeConfiguration;
import io.micronaut.security.oauth2.configuration.OpenIdAdditionalClaimsConfiguration;
import io.micronaut.security.oauth2.endpoint.token.response.DefaultOpenIdUserDetailsMapper;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdTokenResponse;
import org.akhq.configs.Oidc;
import org.akhq.utils.UserGroupUtils;

import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * An OpenID user details mapper that is configurable in the akhq config.
 *
 * It will read a username and roles from the OpenID claims and translate them to akhq roles.
 */
@Singleton
@Replaces(DefaultOpenIdUserDetailsMapper.class)
@Requires(property = "akhq.security.oidc.enabled", value = StringUtils.TRUE)
public class OidcUserDetailsMapper extends DefaultOpenIdUserDetailsMapper {
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
    @Inject
    public OidcUserDetailsMapper(
            OpenIdAdditionalClaimsConfiguration openIdAdditionalClaimsConfiguration,
            AuthenticationModeConfiguration authenticationModeConfiguration,
            Oidc oidc,
            UserGroupUtils userGroupUtils
    ) {
        super(openIdAdditionalClaimsConfiguration, authenticationModeConfiguration);
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
        Map<String, Object> attributes = super.buildAttributes(providerName, tokenResponse, openIdClaims);
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
    @NonNull
    @Override
    public UserDetails createUserDetails(String providerName, OpenIdTokenResponse tokenResponse, OpenIdClaims openIdClaims) {
        String username = getUsername(providerName, tokenResponse, openIdClaims);
        List<String> akhqGroups = getAkhqGroups(providerName, openIdClaims, username);
        List<String> roles = userGroupUtils.getUserRoles(akhqGroups);
        Map<String, Object> attributes = buildAttributes(providerName, tokenResponse, openIdClaims, akhqGroups);

        /**
        * In case of OIDC the user roles are not correctly mapped to corresponding roles in akhq,
        * If we find a groups-field in the user attributes override it with the correctly mapped
        * roles that match the associated akhq group
        */
        Oidc.Provider provider = oidc.getProvider(providerName);
        if (attributes.containsKey(provider.getGroupsField())) {
            attributes.put(provider.getGroupsField(), roles);
        }
        return new UserDetails(username, roles, attributes);
    }
}
