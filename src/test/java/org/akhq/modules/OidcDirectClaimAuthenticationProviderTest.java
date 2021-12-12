package org.akhq.modules;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.security.authentication.*;
import io.micronaut.security.oauth2.client.DefaultOpenIdProviderMetadata;
import io.micronaut.security.oauth2.endpoint.token.request.TokenEndpointClient;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdTokenResponse;
import io.micronaut.security.oauth2.endpoint.token.response.validation.OpenIdTokenResponseValidator;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@MicronautTest(environments = "keycloak")
class OidcDirectClaimAuthenticationProviderTest {

    @Named("oidc")
    @Inject
    AuthenticationProvider oidcProvider;

    @Inject
    TokenEndpointClient tokenEndpointClient;

    @Inject
    OpenIdTokenResponseValidator openIdTokenResponseValidator;

    @Named("oidc")
    @MockBean(TokenEndpointClient.class)
    TokenEndpointClient tokenEndpointClient() {
        return mock(TokenEndpointClient.class);
    }

    @Named("oidc")
    @MockBean(OpenIdTokenResponseValidator.class)
    OpenIdTokenResponseValidator openIdTokenResponseValidator() {
        return mock(OpenIdTokenResponseValidator.class);
    }

    @Named("oidc")
    @MockBean(DefaultOpenIdProviderMetadata.class)
    DefaultOpenIdProviderMetadata defaultOpenIdProviderMetadata() {
        return mock(DefaultOpenIdProviderMetadata.class);
    }

    @Test
    void successSingleOidcGroup() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim(OpenIdClaims.CLAIMS_PREFERRED_USERNAME, "user")
                .claim("roles", List.of("topic/read"))
                .claim("topicsFilterRegexp", List.of("^topic1$", "^topic2$"))
                .claim("connectsFilterRegexp", List.of("^connect1", "^connect2"))
                .claim("consumerGroupsFilterRegexp", List.of("^cg1", "^cg2"))
                .build();
        JWT jwt = new PlainJWT(claimsSet);

        Mockito.when(tokenEndpointClient.sendRequest(ArgumentMatchers.any()))
                .thenReturn(Publishers.just(new OpenIdTokenResponse()));
        Mockito.when(openIdTokenResponseValidator.validate(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(Optional.of(jwt));

        AuthenticationResponse response = Flowable
                .fromPublisher(oidcProvider.authenticate(null, new UsernamePasswordCredentials(
                        "user",
                        "pass"
                ))).blockingFirst();

        assertTrue(response.isAuthenticated());
        assertTrue(response.getAuthentication().isPresent());
        assertEquals("user", response.getAuthentication().get().getName());

        Collection<String> roles = response.getAuthentication().get().getRoles();

        assertThat(roles, hasSize(1));
        assertThat(roles, hasItem("topic/read"));

        Map<String, Object> attributes = response.getAuthentication().get().getAttributes();
        assertThat(attributes.keySet(), hasItem("topicsFilterRegexp"));
        assertThat(attributes.keySet(), hasItem("connectsFilterRegexp"));
        assertThat(attributes.keySet(), hasItem("consumerGroupsFilterRegexp"));

        assertEquals("^topic1$", ((List<?>) attributes.get("topicsFilterRegexp")).get(0));
    }

    @Test
    void successSingleOidcGroup_KeepsAllFilterRegexp() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .claim(OpenIdClaims.CLAIMS_PREFERRED_USERNAME, "user")
            .claim("roles", List.of("topic/read"))
            .claim("topicsFilterRegexp", List.of("^topic1$", "^topic2$"))
            .claim("connectsFilterRegexp", List.of("^connect1", "^connect2"))
            .claim("consumerGroupsFilterRegexp", List.of("^cg1", "^cg2"))
            .claim("futureFilterRegexp", List.of("^future1"))
            .claim("donotkeep", "drop")
            .claim("remove-me", "drop")
            .claim("FilterRegexpRemove", "drop")
            .claim("aaaFilterRegexpaaa", "drop")
            .build();
        JWT jwt = new PlainJWT(claimsSet);

        Mockito.when(tokenEndpointClient.sendRequest(ArgumentMatchers.any()))
            .thenReturn(Publishers.just(new OpenIdTokenResponse()));
        Mockito.when(openIdTokenResponseValidator.validate(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Optional.of(jwt));

        AuthenticationResponse response = Flowable
            .fromPublisher(oidcProvider.authenticate(null, new UsernamePasswordCredentials(
                "user",
                "pass"
            ))).blockingFirst();

        assertTrue(response.isAuthenticated());
        assertTrue(response.getAuthentication().isPresent());
        assertEquals("user", response.getAuthentication().get().getName());

        Collection<String> roles = response.getAuthentication().get().getRoles();

        assertThat(roles, hasSize(1));
        assertThat(roles, hasItem("topic/read"));

        Map<String, Object> attributes = response.getAuthentication().get().getAttributes();

        assertEquals(4, attributes.size());
        assertThat(attributes.keySet(), hasItem("topicsFilterRegexp"));
        assertThat(attributes.keySet(), hasItem("connectsFilterRegexp"));
        assertThat(attributes.keySet(), hasItem("consumerGroupsFilterRegexp"));
        assertThat(attributes.keySet(), hasItem("futureFilterRegexp"));
    }

    @Test
    void failureNoRoles() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .claim(OpenIdClaims.CLAIMS_PREFERRED_USERNAME, "user")
            //.claim("roles", List.of("topic/read")) no roles
            .claim("topicsFilterRegexp", List.of("^topic1$", "^topic2$"))
            .claim("connectsFilterRegexp", List.of("^connect1", "^connect2"))
            .claim("consumerGroupsFilterRegexp", List.of("^cg1", "^cg2"))
            .build();
        JWT jwt = new PlainJWT(claimsSet);

        Mockito.when(tokenEndpointClient.sendRequest(ArgumentMatchers.any()))
            .thenReturn(Publishers.just(new OpenIdTokenResponse()));
        Mockito.when(openIdTokenResponseValidator.validate(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Optional.of(jwt));

        AuthenticationResponse response = Flowable
            .fromPublisher(oidcProvider.authenticate(null, new UsernamePasswordCredentials(
                "user",
                "pass"
            ))).blockingFirst();

        assertThat(response, instanceOf(AuthenticationFailed.class));
    }

    @Test
    void failureRolesNotAList() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .claim(OpenIdClaims.CLAIMS_PREFERRED_USERNAME, "user")
            .claim("roles", "string") //not a list of roles
            .claim("topicsFilterRegexp", List.of("^topic1$", "^topic2$"))
            .claim("connectsFilterRegexp", List.of("^connect1", "^connect2"))
            .claim("consumerGroupsFilterRegexp", List.of("^cg1", "^cg2"))
            .build();
        JWT jwt = new PlainJWT(claimsSet);

        Mockito.when(tokenEndpointClient.sendRequest(ArgumentMatchers.any()))
            .thenReturn(Publishers.just(new OpenIdTokenResponse()));
        Mockito.when(openIdTokenResponseValidator.validate(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Optional.of(jwt));

        AuthenticationResponse response = Flowable
            .fromPublisher(oidcProvider.authenticate(null, new UsernamePasswordCredentials(
                "user",
                "pass"
            ))).blockingFirst();

        assertThat(response, instanceOf(AuthenticationFailed.class));
    }
}
