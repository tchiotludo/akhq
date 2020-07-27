package org.akhq.modules;

import io.micronaut.configuration.security.ldap.ContextAuthenticationMapper;
import io.micronaut.configuration.security.ldap.DefaultContextAuthenticationMapper;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UserDetails;
import org.akhq.configs.LdapGroup;
import org.akhq.configs.LdapUser;
import org.akhq.utils.UserGroupUtils;

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
    private List<LdapGroup> akhqLdapGroups;

    @Inject
    private List<LdapUser> akhqLdapUsers;

    @Inject
    private UserGroupUtils userGroupUtils;

    @Override
    public AuthenticationResponse map(ConvertibleValues<Object> attributes, String username, Set<String> groups) {
        List<String> akhqgroups = getUserAkhqGroups(username, groups);
        return new UserDetails(username, userGroupUtils.getUserRoles(akhqgroups), userGroupUtils.getUserAttributes(akhqgroups));
    }

    /**
     * Get Akhq Groups configured in Ldap groups
     * @param ldapGroups  list of ldap groups associated to the user
     * @return list of Akhq groups configured for the ldap groups. See in application.yml property akhq.security.ldap
     */
    private List<String> getUserAkhqGroups(String username, Set<String> ldapGroups) {
        return Stream.concat(
            this.akhqLdapUsers.stream()
                .filter(ldapUser -> username.equalsIgnoreCase(ldapUser.getUsername()))
                .flatMap(ldapUser -> ldapUser.getGroups().stream()),

            this.akhqLdapGroups.stream()
                .filter(ldapGroup -> ldapGroups.stream().map(String::toLowerCase).anyMatch(s -> s.equals(ldapGroup.getName().toLowerCase())))
                .flatMap(ldapGroup -> ldapGroup.getGroups().stream()))

            .distinct().collect(Collectors.toList());
    }
}
