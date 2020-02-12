package org.kafkahq.modules;

import io.micronaut.configuration.security.ldap.ContextAuthenticationMapper;
import io.micronaut.configuration.security.ldap.DefaultContextAuthenticationMapper;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UserDetails;
import org.kafkahq.configs.LdapGroup;
import org.kafkahq.configs.LdapUser;
import org.kafkahq.utils.UserGroupUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@Replaces(DefaultContextAuthenticationMapper.class)
public class LdapContextAuthenticationMapper implements ContextAuthenticationMapper {

    @Inject
    private List<LdapGroup> kafkaHQLdapGroups;

    @Inject
    private List<LdapUser> kafkaHQLdapUsers;

    @Inject
    private UserGroupUtils userGroupUtils;

    @Override
    public AuthenticationResponse map(ConvertibleValues<Object> attributes, String username, Set<String> groups) {
        List<String> kafkaHQgroups = getUserKafkaHQGroups(username, groups);
        return new UserDetails(username, userGroupUtils.getUserRoles(kafkaHQgroups), userGroupUtils.getUserAttributes(kafkaHQgroups));
    }

    /**
     * Get KafkaHQ Groups configured in Ldap groups
     * @param ldapGroups  list of ldap groups associated to the user
     * @return list of KafkaHQ groups configured for the ldap groups. See in application.yml property kafkahq.security.ldap
     */
    private List<String> getUserKafkaHQGroups(String username, Set<String> ldapGroups) {
        return Stream.concat(
            this.kafkaHQLdapUsers.stream()
                .filter(ldapUser -> username.equalsIgnoreCase(ldapUser.getUsername()))
                .flatMap(ldapUser -> ldapUser.getGroups().stream()),

            this.kafkaHQLdapGroups.stream()
                .filter(ldapGroup -> ldapGroups.stream().map(String::toLowerCase).anyMatch(s -> s.equals(ldapGroup.getName().toLowerCase())))
                .flatMap(ldapGroup -> ldapGroup.getGroups().stream()))

            .distinct().collect(Collectors.toList());
    }
}