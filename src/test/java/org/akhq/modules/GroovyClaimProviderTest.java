package org.akhq.modules;

import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(environments = "groovy")
public class GroovyClaimProviderTest {
    @Inject
    BasicAuthAuthenticationProvider auth;

    @Test
    public void successUser() {
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

        assertThat(roles, hasSize(1));
        assertThat(roles, hasItem("topic/read"));

        assertEquals("single-topic", ((List)userDetail.getAttributes("roles", "username").get("topicsFilterRegexp")).get(0));
    }
}
