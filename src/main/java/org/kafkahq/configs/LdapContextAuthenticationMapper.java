package org.kafkahq.configs;

import io.micronaut.configuration.security.ldap.ContextAuthenticationMapper;
import io.micronaut.configuration.security.ldap.DefaultContextAuthenticationMapper;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UserDetails;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Replaces(DefaultContextAuthenticationMapper.class)
public class LdapContextAuthenticationMapper implements ContextAuthenticationMapper {

    @Inject
    private List<SecurityGroup> securityGroups;

    @Override
    public AuthenticationResponse map(ConvertibleValues<Object> attributes, String username, Set<String> groups) {
        // Get all KafkaHQ Group roles by ldap group name
        List<String> roles = securityGroups.stream()
                    .filter(securityGroup -> groups.contains(securityGroup.getLdapGroup()))
                    .flatMap(securityGroup -> securityGroup.getRoles().stream())
                    .distinct()
                    .collect(Collectors.toList());
        return new UserDetails(username, roles);
    }
}