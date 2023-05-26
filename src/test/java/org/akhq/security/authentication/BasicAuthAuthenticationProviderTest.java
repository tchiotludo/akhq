package org.akhq.security.authentication;

import io.micronaut.security.authentication.*;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class BasicAuthAuthenticationProviderTest {
    @Inject
    BasicAuthAuthenticationProvider auth;

    @Test
    void success() {
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
        assertEquals(roles.get("limited").size(), 4);
    }

    @Test
    void successCase() {
        AuthenticationResponse response = Flowable
            .fromPublisher(auth.authenticate(null, new UsernamePasswordCredentials(
                "MyUser3!@yàhöù.com",
                "pass"
            ))).blockingFirst();


        assertTrue(response.isAuthenticated());
        assertTrue(response.getAuthentication().isPresent());
        assertEquals("MyUser3!@yàhöù.com", response.getAuthentication().get().getName());

        Map<String, List> roles = (Map<String, List>)response.getAuthentication().get().getAttributes().get("groups");

        assertThat(roles.keySet(), hasSize(1));
        assertNotNull(roles.get("limited"));
        assertEquals(roles.get("limited").size(), 4);
    }

    @Test
    void failed_UserNotFound() {
        AuthenticationResponse response = Flowable
            .fromPublisher(auth.authenticate(null, new UsernamePasswordCredentials(
                "user2",
                "pass2"
            ))).blockingFirst();

        assertFalse(response.isAuthenticated());
        AuthenticationFailed authenticationFailed = (AuthenticationFailed) response;
        assertEquals(AuthenticationFailureReason.USER_NOT_FOUND, authenticationFailed.getReason());
    }

    @Test
    void failed_PasswordInvalid() {
        AuthenticationResponse response = Flowable
                .fromPublisher(auth.authenticate(null, new UsernamePasswordCredentials(
                        "user",
                        "invalid-pass"
                ))).blockingFirst();

        assertFalse(response.isAuthenticated());
        AuthenticationFailed authenticationFailed = (AuthenticationFailed) response;
        assertEquals(AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH, authenticationFailed.getReason());
    }
}
