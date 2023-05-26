package org.akhq.security.claim;

import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.akhq.security.authentication.BasicAuthAuthenticationProvider;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

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

        Map<String, List> roles = (Map<String, List>)response.getAuthentication().get().getAttributes().get("groups");

        assertThat(roles.keySet(), hasSize(1));
        assertNotNull(roles.get("limited"));
    }
}
