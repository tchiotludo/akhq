package org.akhq.modules;

import io.micronaut.configuration.security.ldap.ContextAuthenticationMapper;
import io.micronaut.configuration.security.ldap.DefaultContextAuthenticationMapper;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UserDetails;
import org.akhq.configs.Ldap;
import org.akhq.utils.UserGroupUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Set;

@Singleton
@Replaces(DefaultContextAuthenticationMapper.class)
public class LdapContextAuthenticationMapper implements ContextAuthenticationMapper {

    @Inject
    private Ldap ldap;

    @Inject
    private UserGroupUtils userGroupUtils;

    @Override
    public AuthenticationResponse map(ConvertibleValues<Object> attributes, String username, Set<String> groups) {
        List<String> akhqGroups = getUserAkhqGroups(username, groups);
        return new UserDetails(username, userGroupUtils.getUserRoles(akhqGroups), userGroupUtils.getUserAttributes(akhqGroups));
    }

    /**
     * Get Akhq Groups configured in Ldap groups
     * @param ldapGroups  list of ldap groups associated to the user
     * @return list of Akhq groups configured for the ldap groups. See in application.yml property akhq.security.ldap
     */
    private List<String> getUserAkhqGroups(String username, Set<String> ldapGroups) {
        return UserGroupUtils.mapToAkhqGroups(username, ldapGroups, ldap.getGroups(), ldap.getUsers(), ldap.getDefaultGroup());
    }
}
