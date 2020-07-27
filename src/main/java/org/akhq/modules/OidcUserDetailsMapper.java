package org.akhq.modules;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.StringUtils;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.oauth2.configuration.OpenIdAdditionalClaimsConfiguration;
import io.micronaut.security.oauth2.endpoint.token.response.DefaultOpenIdUserDetailsMapper;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdTokenResponse;
import org.akhq.configs.Oidc;
import org.akhq.utils.UserGroupUtils;

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
public class OidcUserDetailsMapper extends DefaultOpenIdUserDetailsMapper {
    private final UserGroupUtils userGroupUtils;
    private final Oidc oidc;

    @Value("${akhq.security.default-group}")
    private String defaultGroups;

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
        super(openIdAdditionalClaimsConfiguration);
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
    @Override
    protected String getUsername(String providerName, OpenIdTokenResponse tokenResponse, OpenIdClaims openIdClaims) {
        Oidc.Provider provider = oidc.getProvider(providerName);
        return Objects.toString(openIdClaims.get(provider.getUsernameField()));
    }

    /**
     * Tries to read roles from the configured roles field and translates them using {@link UserGroupUtils}.
     * If the configured field cannot be found or isn't some kind of collection, it will use the default group.
     *
     * @param providerName The OpenID provider name
     * @param tokenResponse The token response
     * @param openIdClaims The OpenID claims
     * @return The roles to set in the {@link UserDetails}
     */
    @Override
    protected List<String> getRoles(String providerName, OpenIdTokenResponse tokenResponse, OpenIdClaims openIdClaims) {
        return userGroupUtils.getUserRoles(getOidcRoles(providerName, openIdClaims));
    }
    /**
     * Adds the configured attributes for the user roles to the user attributes.
     *
     * @param providerName The OpenID provider name
     * @param tokenResponse The token response
     * @param openIdClaims The OpenID claims
     * @return The attributes to set in the {@link UserDetails}
     */
    @Override
    protected Map<String, Object> buildAttributes(String providerName, OpenIdTokenResponse tokenResponse, OpenIdClaims openIdClaims) {
        Map<String, Object> attributes = super.buildAttributes(providerName, tokenResponse, openIdClaims);
        userGroupUtils.getUserAttributes(getOidcRoles(providerName, openIdClaims)).forEach(attributes::put);
        return attributes;
    }

    /**
     * Tries to read roles from the configured roles field.
     * If the configured field cannot be found or isn't some kind of collection, it will return the default group.
     *
     * @param providerName The OpenID provider name
     * @param openIdClaims The OpenID claims
     * @return The roles from oidc
     */
    protected List<String> getOidcRoles(String providerName, OpenIdClaims openIdClaims) {
        Oidc.Provider provider = oidc.getProvider(providerName);
        List<String> roles = Collections.singletonList(defaultGroups);
        if (openIdClaims.contains(provider.getRolesField())) {
            Object rolesField = openIdClaims.get(provider.getRolesField());
            if (rolesField instanceof Collection) {
                roles = ((Collection<Object>) rolesField)
                        .stream()
                        .map(Objects::toString)
                        .collect(Collectors.toList());
            }
        }
        return roles;
    }
}
