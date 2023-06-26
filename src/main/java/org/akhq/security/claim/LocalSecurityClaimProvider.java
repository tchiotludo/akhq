package org.akhq.security.claim;

import io.micronaut.context.annotation.Secondary;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.security.*;
import org.akhq.configs.security.ldap.GroupMapping;
import org.akhq.configs.security.ldap.Ldap;
import org.akhq.configs.security.ldap.UserMapping;
import org.akhq.models.security.ClaimProvider;
import org.akhq.models.security.ClaimRequest;
import org.akhq.models.security.ClaimResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Singleton
@Secondary
public class LocalSecurityClaimProvider implements ClaimProvider {

    @Inject
    SecurityProperties securityProperties;
    @Inject
    HeaderAuth headerAuthProperties;
    @Inject
    Ldap ldapProperties;
    @Inject
    Oidc oidcProperties;
    @Inject
    Oauth oauthProperties;

    @Override
    public ClaimResponse generateClaim(ClaimRequest request) {
        List<UserMapping> userMappings;
        List<GroupMapping> groupMappings;
        String defaultGroup;
        List<String> akhqGroups = new ArrayList<>();
        switch (request.getProviderType()) {
            case BASIC_AUTH:
                // we already have target AKHQ groups
                akhqGroups.addAll(request.getGroups());
                break;
            case HEADER:
                // we need to convert from externally provided groups to AKHQ groups to find the roles and attributes
                // using akhq.security.header-auth.groups and akhq.security.header-auth.users
                // as well as akhq.security.header-auth.default-group
                userMappings = headerAuthProperties.getUsers();
                groupMappings = headerAuthProperties.getGroups();
                defaultGroup = headerAuthProperties.getDefaultGroup();
                akhqGroups.addAll(mapToAkhqGroups(request.getUsername(), request.getGroups(), groupMappings, userMappings, defaultGroup));
                break;
            case LDAP:
                // we need to convert from LDAP groups to AKHQ groups to find the roles and attributes
                // using akhq.security.ldap.groups and akhq.security.ldap.users
                // as well as akhq.security.ldap.default-group
                userMappings = ldapProperties.getUsers();
                groupMappings = ldapProperties.getGroups();
                defaultGroup = ldapProperties.getDefaultGroup();
                akhqGroups.addAll(mapToAkhqGroups(request.getUsername(), request.getGroups(), groupMappings, userMappings, defaultGroup));
                break;
            case OIDC:
                // we need to convert from OIDC groups to AKHQ groups to find the roles and attributes
                // using akhq.security.oidc.groups and akhq.security.oidc.users
                // as well as akhq.security.oidc.default-group
                Oidc.Provider provider = oidcProperties.getProvider(request.getProviderName());
                userMappings = provider.getUsers();
                groupMappings = provider.getGroups();
                defaultGroup = provider.getDefaultGroup();
                akhqGroups.addAll(mapToAkhqGroups(request.getUsername(), request.getGroups(), groupMappings, userMappings, defaultGroup));
                break;
            case OAUTH:
                // we need to convert from OAUTH login name to AKHQ groups to find the roles and attributes
                // using akhq.security.oauth2.groups and akhq.security.oauth2.users
                // as well as akhq.security.oauth2.default-group
                Oauth.Provider oauthPropertiesProvider = oauthProperties.getProvider(request.getProviderName());
                userMappings = oauthPropertiesProvider.getUsers();
                groupMappings = oauthPropertiesProvider.getGroups();
                defaultGroup = oauthPropertiesProvider.getDefaultGroup();
                akhqGroups.addAll(mapToAkhqGroups(request.getUsername(), request.getGroups(), groupMappings, userMappings, defaultGroup));
                break;
            default:
                break;
        }

        Map<String, List<Group>> groups = securityProperties.getGroups().entrySet().stream()
            .filter(entry -> akhqGroups.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // translate akhq groups into roles and attributes
        return ClaimResponse.builder().groups(groups).build();
    }

    /**
     * Maps the provider username and a set of provider groups to AKHQ groups using group and user mappings.
     *
     * @param username       the username to use
     * @param providerGroups the groups from the provider side
     * @param groupMappings  the group mappings configured for the provider
     * @param userMappings   the user mappings configured for the provider
     * @param defaultGroup   a default group for the provider
     * @return the mapped AKHQ groups
     */
    public List<String> mapToAkhqGroups(
            String username,
            List<String> providerGroups,
            List<GroupMapping> groupMappings,
            List<UserMapping> userMappings,
            String defaultGroup) {
        Stream<String> defaultGroupStream = StringUtils.hasText(defaultGroup) ? Stream.of(defaultGroup) : Stream.empty();
        return Stream.concat(
                Stream.concat(
                        userMappings.stream()
                                .filter(mapping -> username.equalsIgnoreCase(mapping.getUsername()))
                                .flatMap(mapping -> mapping.getGroups().stream()),
                        groupMappings.stream()
                                .filter(mapping -> providerGroups.stream().anyMatch(s -> s.equalsIgnoreCase(mapping.getName())))
                                .flatMap(mapping -> mapping.getGroups().stream())
                ),
                defaultGroupStream
        ).distinct().collect(Collectors.toList());
    }
}
