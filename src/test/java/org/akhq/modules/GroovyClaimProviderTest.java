package org.akhq.modules;

import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(environments = "groovy")
class GroovyClaimProviderTest {
    @Inject
    BasicAuthAuthenticationProvider auth;

    @Test
    void successUser() {
        AuthenticationResponse response = Flowable
                .fromPublisher(auth.authenticate(null, new UsernamePasswordCredentials(
                        "user",
                        "pass"
                ))).blockingFirst();


        assertTrue(response.isAuthenticated());
        assertTrue(response.getAuthentication().isPresent());
        assertEquals("user", response.getAuthentication().get().getName());

        Collection<String> roles = response.getAuthentication().get().getRoles();

        assertThat(roles, hasSize(1));
        assertThat(roles, hasItem("topic/read"));

        assertEquals("single-topic", ((List<?>) response.getAuthentication().get().getAttributes().get("topicsFilterRegexp")).get(0));
    }
}
