package org.akhq.modules;

import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.reactivex.Flowable;
import org.akhq.AbstractTest;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class BasicAuthAuthenticationProviderTest extends AbstractTest {
    @Inject
    BasicAuthAuthenticationProvider auth;

    @Test
    public void success() {
        AuthenticationResponse response = Flowable
            .fromPublisher(auth.authenticate(null, new UsernamePasswordCredentials(
                "user",
                "pass"
            ))).blockingFirst();

        assertThat(response, instanceOf(UserDetails.class));

        UserDetails userDetail = (UserDetails) response;

        assertTrue(userDetail.isAuthenticated());
        assertEquals("user", userDetail.getUsername());

        Collection<String> roles = userDetail.getRoles();

        assertThat(roles, hasSize(4));
        assertThat(roles, hasItem("topic/read"));
        assertThat(roles, hasItem("registry/version/delete"));

        assertEquals("test.*", ((List)userDetail.getAttributes("roles", "username").get("topicsFilterRegexp")).get(0));
    }

    @Test
    public void failed() {
        AuthenticationResponse response = Flowable
            .fromPublisher(auth.authenticate(null, new UsernamePasswordCredentials(
                "user2",
                "pass2"
            ))).blockingFirst();

        assertFalse(response.isAuthenticated());
    }
}
