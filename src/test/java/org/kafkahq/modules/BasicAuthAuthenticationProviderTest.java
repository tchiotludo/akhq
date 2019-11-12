package org.kafkahq.modules;

import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;
import org.kafkahq.AbstractTest;

import javax.inject.Inject;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class BasicAuthAuthenticationProviderTest extends AbstractTest {
    @Inject
    BasicAuthAuthenticationProvider auth;

    @Test
    public void success() {
        AuthenticationResponse response = Flowable
            .fromPublisher(auth.authenticate(new UsernamePasswordCredentials(
                "user",
                "pass"
            ))).blockingFirst();

        assertThat(response, instanceOf(UserDetails.class));

        UserDetails userDetail = (UserDetails) response;

        assertTrue(userDetail.isAuthenticated());
        assertEquals("user", userDetail.getUsername());
        assertEquals("test.*", userDetail.getAttributes("roles", "user").get("topics-filter-regexp"));


        Collection<String> roles = userDetail.getRoles();

        assertThat(roles, hasSize(4));
        assertThat(roles, hasItem("topic/read"));
        assertThat(roles, hasItem("registry/version/delete"));
    }

    @Test
    public void failed() {
        AuthenticationResponse response = Flowable
            .fromPublisher(auth.authenticate(new UsernamePasswordCredentials(
                "user2",
                "pass2"
            ))).blockingFirst();

        assertFalse(response.isAuthenticated());
    }
}
