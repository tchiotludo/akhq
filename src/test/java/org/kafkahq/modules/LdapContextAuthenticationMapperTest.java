package org.kafkahq.modules;

import io.micronaut.configuration.security.ldap.context.AttributesConvertibleValues;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;
import org.kafkahq.AbstractTest;

import javax.inject.Inject;
import javax.naming.directory.BasicAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class LdapContextAuthenticationMapperTest extends AbstractTest {
    @Inject
    LdapContextAuthenticationMapper mapper;

    @Test
    public void map() {

        AuthenticationResponse response = mapper.map( new AttributesConvertibleValues(new BasicAttributes()),"user",  Collections.singleton("ldap-admin"));

        assertThat(response, instanceOf(UserDetails.class));

        UserDetails userDetail = (UserDetails) response;

        assertTrue(userDetail.isAuthenticated());
        assertEquals("user", userDetail.getUsername());

        Collection<String> roles = userDetail.getRoles();

        assertThat(roles, hasSize(4));
        assertThat(roles, hasItem("topic/read"));
        assertThat(roles, hasItem("registry/version/delete"));

        assertEquals("test.*", ((List)userDetail.getAttributes("roles", "username").get("topics-filter-regexp")).get(0));
    }


}
