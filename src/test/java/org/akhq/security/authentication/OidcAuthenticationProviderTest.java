package org.akhq.security.authentication;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import io.micronaut.context.BeanProvider;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.security.authentication.*;
import io.micronaut.security.authentication.provider.ReactiveAuthenticationProvider;
import io.micronaut.security.oauth2.client.DefaultOpenIdProviderMetadata;
import io.micronaut.security.oauth2.endpoint.token.request.TokenEndpointClient;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdTokenResponse;
import io.micronaut.security.oauth2.endpoint.token.response.validation.ReactiveOpenIdTokenResponseValidator;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.akhq.configs.security.Group;
import org.akhq.controllers.AkhqController;
import org.akhq.models.security.ClaimProvider;
import org.akhq.security.rule.AKHQSecurityRule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@MicronautTest(environments = "oidc")
class OidcAuthenticationProviderTest {

    @SuppressWarnings("rawtypes")
    @Named("oidc")
    @Inject
    BeanProvider<ReactiveAuthenticationProvider> oidcProvider;

    @Inject
    TokenEndpointClient tokenEndpointClient;

    @Inject
    ReactiveOpenIdTokenResponseValidator openIdTokenResponseValidator;

    @Inject
    AkhqController akhqController;

    @Inject
    private ClaimProvider claimProvider;

    @Named("oidc")
    @MockBean(TokenEndpointClient.class)
    TokenEndpointClient tokenEndpointClient() {
        return mock(TokenEndpointClient.class);
    }

    @Named("oidc")
    @MockBean(ReactiveOpenIdTokenResponseValidator.class)
    ReactiveOpenIdTokenResponseValidator openIdTokenResponseValidator() {
        return mock(ReactiveOpenIdTokenResponseValidator.class);
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
                .thenReturn(Publishers.just(jwt));

        AuthenticationResponse response = (AuthenticationResponse) Mono
                .from(oidcProvider.get().authenticate(null, new UsernamePasswordCredentials(
                        "user",
                        "pass"
                ))).block();

        assertTrue(response.isAuthenticated());
        assertTrue(response.getAuthentication().isPresent());
        assertEquals("user", response.getAuthentication().get().getName());

        Map<String, List<Group>> groups = getGroups(response);

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
                .thenReturn(Publishers.just(jwt));

        AuthenticationResponse response = (AuthenticationResponse) Mono
                .from(oidcProvider.get().authenticate(null, new UsernamePasswordCredentials(
                        "user",
                        "pass"
                ))).block();

        assertTrue(response.isAuthenticated());
        assertTrue(response.getAuthentication().isPresent());
        assertEquals("user", response.getAuthentication().get().getName());

        Map<String, List<Group>> groups = getGroups(response);

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
                .thenReturn(Publishers.just(jwt));

        AuthenticationResponse response = (AuthenticationResponse) Mono
                .from(oidcProvider.get().authenticate(null, new UsernamePasswordCredentials(
                        "user",
                        "pass"
                ))).block();

        assertTrue(response.isAuthenticated());
        assertTrue(response.getAuthentication().isPresent());
        assertEquals("user", response.getAuthentication().get().getName());

        Map<String, List<Group>> groups = getGroups(response);

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
                .thenReturn(Publishers.just(jwt));

        AuthenticationResponse response = (AuthenticationResponse) Mono
                .from(oidcProvider.get().authenticate(null, new UsernamePasswordCredentials(
                        "user2",
                        "pass"
                ))).block();

        assertTrue(response.isAuthenticated());
        assertTrue(response.getAuthentication().isPresent());
        assertEquals("user2", response.getAuthentication().get().getName());

        Map<String, List<Group>> groups = getGroups(response);

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

    private Map<String, List<Group>> getGroups(final AuthenticationResponse response) {
        return AKHQSecurityRule.unrollGroups(response.getAuthentication().get(), claimProvider);
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
                .thenReturn(Publishers.just(jwt));

        AuthenticationResponse response = (AuthenticationResponse) Mono
                .from(oidcProvider.get().authenticate(null, new UsernamePasswordCredentials(
                        "user",
                        "pass"
                ))).block();

        assertTrue(response.isAuthenticated());
        assertEquals("user", response.getAuthentication().get().getName());

        Map<String, List<Group>> roles = getGroups(response);

        assertThat(roles.keySet(), hasSize(0));
    }

    @Test
    void failure() {
        Mockito.when(tokenEndpointClient.sendRequest(ArgumentMatchers.any()))
                .thenReturn(Publishers.just(new OpenIdTokenResponse()));
        Mockito.when(openIdTokenResponseValidator.validate(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(Mono.empty());
        Object response = Mono
            .from(oidcProvider.get().authenticate(null, new UsernamePasswordCredentials(
                "user",
                "pass"
            )))
            .block();
        assertNull(response);
    }

    @Test
    void noLoginForm() {
        AkhqController.AuthDefinition actual = akhqController.auths();

        assertTrue(actual.isLoginEnabled(), "Login must be enabled with OIDC");
        assertFalse(actual.isFormEnabled(), "Login Form must not be active if only OIDC is enabled");
        assertFalse(actual.getOidcAuths().isEmpty());
    }
}
