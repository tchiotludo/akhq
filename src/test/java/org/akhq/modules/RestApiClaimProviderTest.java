package org.akhq.modules;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.akhq.utils.ClaimProvider;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
@Property(name = "akhq.security.rest.enabled", value = "true")
@Property(name = "akhq.security.rest.url", value = "/external-role-api")
public class RestApiClaimProviderTest {
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

        assertThat(roles, hasSize(2));
        assertThat(roles, hasItem("topic/read"));
        assertThat(roles, hasItem("topic/write"));

        assertEquals(".*", ((List)userDetail.getAttributes("roles", "username").get("topicsFilterRegexp")).get(0));
    }
}
