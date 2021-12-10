package org.akhq.utils;

import io.micronaut.context.annotation.Secondary;
import io.micronaut.core.util.StringUtils;
import org.akhq.configs.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Override
    public AKHQClaimResponse generateClaim(AKHQClaimRequest request) {
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
            default:
                break;
        }

        // translate akhq groups into roles and attributes
        return generateClaimFromAKHQGroups(request.getUsername(), akhqGroups);
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

    public AKHQClaimResponse generateClaimFromAKHQGroups(String username, List<String> groups) {
        return AKHQClaimResponse.builder()
                .roles(getUserRoles(groups))
                .attributes(
                        Map.of(
                                "topicsFilterRegexp", getAttributeMergedList(groups, "topicsFilterRegexp"),
                                "connectsFilterRegexp", getAttributeMergedList(groups, "connectsFilterRegexp"),
                                "consumerGroupsFilterRegexp", getAttributeMergedList(groups, "consumerGroupsFilterRegexp")
                        )
                )
                .build();
    }

    /**
     * Get all distinct roles for the list of groups
     *
     * @param groups list of user groups
     * @return list of roles
     */
    public List<String> getUserRoles(List<String> groups) {
        if (securityProperties.getGroups() == null || groups == null) {
            return List.of();
        }

        return securityProperties.getGroups().values().stream()
                .filter(group -> groups.contains(group.getName()))
                .filter(group -> group.getRoles() != null)
                .flatMap(group -> group.getRoles().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> getAttributeMergedList(List<String> groups, String attribute) {
        return securityProperties.getGroups().values().stream()
                //group matches
                .filter(group -> groups.contains(group.getName()))
                //group contains this attribute in the attributes Map
                .filter(group -> group.getAttributes() != null && group.getAttributes().containsKey(attribute))
                // attribute is not an empty List
                .filter(group -> group.getAttributes().get(attribute) != null && !group.getAttributes().get(attribute).isEmpty())
                //flatMap attribute List
                .flatMap(group -> group.getAttributes().get(attribute).stream())
                //dedup & collect
                .distinct()
                .collect(Collectors.toList());
    }
}
