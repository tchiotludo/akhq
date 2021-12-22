package org.akhq.utils;

import io.micronaut.context.annotation.Secondary;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.*;
import org.akhq.configs.newAcls.Binding;

import java.util.ArrayList;
import java.util.List;
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

    @Override
    public ClaimResponse generateClaim(ClaimRequest request) {
        List<UserMapping> userMappings;
        List<GroupMapping> groupMappings;
        List<String> defaultBindings;
        List<String> bindingNames = new ArrayList<>();
        switch (request.getProviderType()) {
            case BASIC_AUTH:
                // we already have target AKHQ bindings
                bindingNames.addAll(request.getGroups());
                break;
            case HEADER:
                // we need to convert from externally provided groups to AKHQ bindings to find the roles and attributes
                // using akhq.security.header-auth.groups and akhq.security.header-auth.users
                // as well as akhq.security.header-auth.default-group
                userMappings = headerAuthProperties.getUsers();
                groupMappings = headerAuthProperties.getGroups();
                defaultBindings = headerAuthProperties.getDefaultBindings();
                bindingNames.addAll(mapToAkhqGroups(request.getUsername(), request.getGroups(), groupMappings, userMappings, defaultBindings));
                break;
            case LDAP:
                // we need to convert from LDAP groups to AKHQ groups to find the roles and attributes
                // using akhq.security.ldap.groups and akhq.security.ldap.users
                // as well as akhq.security.ldap.default-group
                userMappings = ldapProperties.getUsers();
                groupMappings = ldapProperties.getGroups();
                defaultBindings = ldapProperties.getDefaultBindings();
                bindingNames.addAll(mapToAkhqGroups(request.getUsername(), request.getGroups(), groupMappings, userMappings, defaultBindings));
                break;
            case OIDC:
                // we need to convert from OIDC groups to AKHQ groups to find the roles and attributes
                // using akhq.security.oidc.groups and akhq.security.oidc.users
                // as well as akhq.security.oidc.default-group
                Oidc.Provider provider = oidcProperties.getProvider(request.getProviderName());
                userMappings = provider.getUsers();
                groupMappings = provider.getGroups();
                defaultBindings = provider.getDefaultBindings();
                bindingNames.addAll(mapToAkhqGroups(request.getUsername(), request.getGroups(), groupMappings, userMappings, defaultBindings));
                break;
            default:
                break;
        }
        List<Binding> bindings = bindingNames.stream()
            .flatMap(bindingName -> {
                if (securityProperties.getBindings().containsKey(bindingName)) {
                    return securityProperties.getBindings().get(bindingName).stream();
                } else {
                    // TODO warn or fail ?
                    log.warn("Binding {} not found for user {}", bindingName, request.getUsername());
                    return Stream.empty();
                }
            })
            .collect(Collectors.toList());

        return ClaimResponse.builder()
            .bindings(bindings)
            .build();
    }

    /**
     * Maps the provider username and a set of provider groups to AKHQ groups using group and user mappings.
     *
     * @param username        the username to use
     * @param providerGroups  the groups from the provider side
     * @param groupMappings   the group mappings configured for the provider
     * @param userMappings    the user mappings configured for the provider
     * @param defaultBindings the default bindings for the provider
     * @return the mapped AKHQ groups
     */
    public List<String> mapToAkhqGroups(
            String username,
            List<String> providerGroups,
            List<GroupMapping> groupMappings,
            List<UserMapping> userMappings,
            List<String> defaultBindings) {
        Stream<String> defaultBindingsStream = defaultBindings.stream();
        return Stream.concat(
                Stream.concat(
                        userMappings.stream()
                                .filter(mapping -> username.equalsIgnoreCase(mapping.getUsername()))
                                .flatMap(mapping -> mapping.getBindings().stream()),
                        groupMappings.stream()
                                .filter(mapping -> providerGroups.stream().anyMatch(s -> s.equalsIgnoreCase(mapping.getName())))
                                .flatMap(mapping -> mapping.getBindings().stream())
                ),
            defaultBindingsStream
        ).distinct().collect(Collectors.toList());
    }
}
