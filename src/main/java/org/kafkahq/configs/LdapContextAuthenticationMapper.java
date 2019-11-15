package org.kafkahq.configs;

import io.micronaut.configuration.security.ldap.ContextAuthenticationMapper;
import io.micronaut.configuration.security.ldap.DefaultContextAuthenticationMapper;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UserDetails;
import org.kafkahq.utils.UserGroupUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
@Replaces(DefaultContextAuthenticationMapper.class)
public class LdapContextAuthenticationMapper implements ContextAuthenticationMapper {

    @Inject
    private List<LdapGroup> kafkaHQLdapGroups;

    @Inject
    private UserGroupUtils userGroupUtils;

    @Override
    public AuthenticationResponse map(ConvertibleValues<Object> attributes, String username, Set<String> groups) {

        List<String> kafkaHQgroups = getUserKafkaHQGroups(groups);
        return new UserDetails(username, userGroupUtils.getUserRoles(kafkaHQgroups), userGroupUtils.getUserAttributes(kafkaHQgroups));
    }

    private List<String> getUserKafkaHQGroups(Set<String> ldapGroups) {

        return this.kafkaHQLdapGroups.stream()
                .filter(ldapGroup -> ldapGroups.stream().map(String::toLowerCase).anyMatch(s -> s.equals(ldapGroup.getName().toLowerCase())) )
                .flatMap(ldapGroup -> ldapGroup.groups.stream())
                .distinct()
                .collect(Collectors.toList());
    }



}