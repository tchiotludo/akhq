package org.akhq.security.authentication;

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
import org.akhq.controllers.AkhqController;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@MicronautTest(environments = "oidc")
class OidcAuthenticationProviderTest {

    @Named("oidc")
    @Inject
    AuthenticationProvider oidcProvider;

    @Inject
    TokenEndpointClient tokenEndpointClient;

    @Inject
    OpenIdTokenResponseValidator openIdTokenResponseValidator;

    @Inject
    AkhqController akhqController;

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
                .claim("roles", List.of("oidc-limited-group"))
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

        Map<String, List<Group>> groups = (Map<String, List<Group>>)response.getAuthentication().get().getAttributes().get("groups");

        assertThat(groups.keySet(), hasSize(1));
        assertNotNull(groups.get("limited"));
        assertEquals(3, groups.get("limited").size());
        assertThat(groups.get("limited").stream().map(Group::getRole).collect(Collectors.toList()),
            containsInAnyOrder("topic-read", "topic-write", "schema-delete"));
        assertThat(groups.get("limited").stream().map(Group::getClusters).flatMap(Collection::stream).collect(Collectors.toList()),
            containsInAnyOrder("pub.*", "pub.*", "pub.*"));
        assertThat(groups.get("limited").stream().map(Group::getPatterns).flatMap(Collection::stream).collect(Collectors.toList()),
            containsInAnyOrder("test.*", "test.*", "user.*"));
    }

    @Test
    void successSingleStringOidcGroup() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim(OpenIdClaims.CLAIMS_PREFERRED_USERNAME, "user")
                .claim("roles", "oidc-limited-group")
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

        Map<String, List<Group>> groups = (Map<String, List<Group>>)response.getAuthentication().get().getAttributes().get("groups");

        assertThat(groups.keySet(), hasSize(1));
        assertNotNull(groups.get("limited"));
        assertEquals(3, groups.get("limited").size());
        assertThat(groups.get("limited").stream().map(Group::getRole).collect(Collectors.toList()),
            containsInAnyOrder("topic-read", "topic-write", "schema-delete"));
        assertThat(groups.get("limited").stream().map(Group::getClusters).flatMap(Collection::stream).collect(Collectors.toList()),
            containsInAnyOrder("pub.*", "pub.*", "pub.*"));
        assertThat(groups.get("limited").stream().map(Group::getPatterns).flatMap(Collection::stream).collect(Collectors.toList()),
            containsInAnyOrder("test.*", "test.*", "user.*"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void successWithMultipleOidcGroups() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim(OpenIdClaims.CLAIMS_PREFERRED_USERNAME, "user")
                .claim("roles", List.of("oidc-limited-group", "oidc-operator-group"))
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

        Map<String, List<Group>> groups = (Map<String, List<Group>>)response.getAuthentication().get().getAttributes().get("groups");

        assertThat(groups.keySet(), hasSize(2));
        assertNotNull(groups.get("limited"));
        assertEquals(3, groups.get("limited").size());
        assertThat(groups.get("limited").stream().map(Group::getRole).collect(Collectors.toList()),
            containsInAnyOrder("topic-read", "topic-write", "schema-delete"));
        assertThat(groups.get("limited").stream().map(Group::getClusters).flatMap(Collection::stream).collect(Collectors.toList()),
            containsInAnyOrder("pub.*", "pub.*", "pub.*"));
        assertThat(groups.get("limited").stream().map(Group::getPatterns).flatMap(Collection::stream).collect(Collectors.toList()),
            containsInAnyOrder("test.*", "test.*", "user.*"));

        assertNotNull(groups.get("operator"));
        assertEquals(2, groups.get("operator").size());
        assertThat(groups.get("operator").stream().map(Group::getRole).collect(Collectors.toList()),
            containsInAnyOrder("topic-read", "topic-data-admin"));
        assertThat(groups.get("operator").stream().map(Group::getClusters).flatMap(Collection::stream).collect(Collectors.toList()),
            containsInAnyOrder(".*", ".*"));
        assertThat(groups.get("operator").stream().map(Group::getPatterns).flatMap(Collection::stream).collect(Collectors.toList()),
            containsInAnyOrder("test-operator.*", "test-operator.*"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void successWithOidcGroupAndUserRole() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim(OpenIdClaims.CLAIMS_PREFERRED_USERNAME, "user2")
                .claim("roles", List.of("oidc-limited-group"))
                .build();
        JWT jwt = new PlainJWT(claimsSet);

        Mockito.when(tokenEndpointClient.sendRequest(ArgumentMatchers.any()))
                .thenReturn(Publishers.just(new OpenIdTokenResponse()));
        Mockito.when(openIdTokenResponseValidator.validate(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(Optional.of(jwt));

        AuthenticationResponse response = Flowable
                .fromPublisher(oidcProvider.authenticate(null, new UsernamePasswordCredentials(
                        "user2",
                        "pass"
                ))).blockingFirst();

        assertTrue(response.isAuthenticated());
        assertTrue(response.getAuthentication().isPresent());
        assertEquals("user2", response.getAuthentication().get().getName());

        Map<String, List<Group>> groups = (Map<String, List<Group>>)response.getAuthentication().get().getAttributes().get("groups");

        assertThat(groups.keySet(), hasSize(2));
        assertNotNull(groups.get("limited"));
        assertEquals(3, groups.get("limited").size());
        assertThat(groups.get("limited").stream().map(Group::getRole).collect(Collectors.toList()),
            containsInAnyOrder("topic-read", "topic-write", "schema-delete"));
        assertThat(groups.get("limited").stream().map(Group::getClusters).flatMap(Collection::stream).collect(Collectors.toList()),
            containsInAnyOrder("pub.*", "pub.*", "pub.*"));
        assertThat(groups.get("limited").stream().map(Group::getPatterns).flatMap(Collection::stream).collect(Collectors.toList()),
            containsInAnyOrder("test.*", "test.*", "user.*"));

        assertNotNull(groups.get("operator"));
        assertEquals(2, groups.get("operator").size());
        assertThat(groups.get("operator").stream().map(Group::getRole).collect(Collectors.toList()),
            containsInAnyOrder("topic-read", "topic-data-admin"));
        assertThat(groups.get("operator").stream().map(Group::getClusters).flatMap(Collection::stream).collect(Collectors.toList()),
            containsInAnyOrder(".*", ".*"));
        assertThat(groups.get("operator").stream().map(Group::getPatterns).flatMap(Collection::stream).collect(Collectors.toList()),
            containsInAnyOrder("test-operator.*", "test-operator.*"));
    }

    @Test
    void successWithoutRoles() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim(OpenIdClaims.CLAIMS_PREFERRED_USERNAME, "user")
                .claim("roles", List.of("oidc-other-group"))
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
        assertEquals("user", response.getAuthentication().get().getName());

        Map<String, List> roles = (Map<String, List>)response.getAuthentication().get().getAttributes().get("groups");

        assertThat(roles.keySet(), hasSize(0));
    }

    @Test
    void failure() {

        Mockito.when(tokenEndpointClient.sendRequest(ArgumentMatchers.any()))
                .thenReturn(Publishers.just(new OpenIdTokenResponse()));
        Mockito.when(openIdTokenResponseValidator.validate(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(Optional.empty());

        AuthenticationException authenticationException = assertThrows(AuthenticationException.class, () -> Flowable
                .fromPublisher(oidcProvider.authenticate(null, new UsernamePasswordCredentials(
                        "user",
                        "pass"
                ))).blockingFirst());

        assertThat(authenticationException.getResponse(), instanceOf(AuthenticationFailed.class));
        assertNotNull(authenticationException.getResponse());
        assertFalse(authenticationException.getResponse().isAuthenticated());
    }

    @Test
    void noLoginForm() {
        AkhqController.AuthDefinition actual = akhqController.auths();

        assertTrue(actual.isLoginEnabled(), "Login must be enabled with OIDC");
        assertFalse(actual.isFormEnabled(), "Login Form must not be active if only OIDC is enabled");
        assertFalse(actual.getOidcAuths().isEmpty());
    }
}
