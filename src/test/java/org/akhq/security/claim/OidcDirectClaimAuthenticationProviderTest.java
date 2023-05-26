package org.akhq.security.claim;

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
import org.akhq.configs.security.Group;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import jakarta.inject.Named;
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
        Group group = new Group();
        group.setRole("topic-read");

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim(OpenIdClaims.CLAIMS_PREFERRED_USERNAME, "user")
                .claim("groups", Map.of("limited", List.of(group)))
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

        Map<String, List> roles = (Map<String, List>)response.getAuthentication().get().getAttributes().get("groups");

        assertThat(roles.keySet(), hasSize(1));
        assertNotNull(roles.get("limited"));
    }

    @Test
    void failureNoRoles() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .claim(OpenIdClaims.CLAIMS_PREFERRED_USERNAME, "user")
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
