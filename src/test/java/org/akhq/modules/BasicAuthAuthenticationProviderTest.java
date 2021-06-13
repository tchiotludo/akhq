package org.akhq.modules;

import io.micronaut.security.authentication.*;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class BasicAuthAuthenticationProviderTest {
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
    public void successCase() {
        AuthenticationResponse response = Flowable
            .fromPublisher(auth.authenticate(null, new UsernamePasswordCredentials(
                "MyUser3!@yàhöù.com",
                "pass"
            ))).blockingFirst();

        assertThat(response, instanceOf(UserDetails.class));

        UserDetails userDetail = (UserDetails) response;

        assertTrue(userDetail.isAuthenticated());
        assertEquals("MyUser3!@yàhöù.com", userDetail.getUsername());

        Collection<String> roles = userDetail.getRoles();

        assertThat(roles, hasSize(4));
        assertThat(roles, hasItem("topic/read"));
        assertThat(roles, hasItem("registry/version/delete"));

        assertEquals("test.*", ((List)userDetail.getAttributes("roles", "username").get("topicsFilterRegexp")).get(0));
    }

    @Test
    public void failed_UserNotFound() {
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
    public void failed_PasswordInvalid() {
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
