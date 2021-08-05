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
import org.akhq.controllers.AkhqController;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@MicronautTest(environments = "oidc")
public class OidcAuthenticationProviderTest {

    @Named("oidc")
    @Inject
    AuthenticationProvider oidcProvider;

    @Inject
    TokenEndpointClient tokenEndpointClient;

    @Inject
    OpenIdTokenResponseValidator openIdTokenResponseValidator;

    @Inject
    DefaultOpenIdProviderMetadata defaultOpenIdProviderMetadata;

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

        assertThat(response, instanceOf(UserDetails.class));

        UserDetails userDetail = (UserDetails) response;

        assertTrue(userDetail.isAuthenticated());
        assertEquals("user", userDetail.getUsername());

        Collection<String> roles = userDetail.getRoles();

        assertThat(roles, hasSize(4));
        assertThat(roles, hasItem("topic/read"));
        assertThat(roles, hasItem("registry/version/delete"));

        assertEquals("test.*", ((List) userDetail.getAttributes("roles", "username").get("topicsFilterRegexp")).get(0));

    }

    @Test
    public void successWithMultipleOidcGroups() {

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

        assertThat(response, instanceOf(UserDetails.class));

        UserDetails userDetail = (UserDetails) response;

        assertTrue(userDetail.isAuthenticated());
        assertEquals("user", userDetail.getUsername());

        Collection<String> roles = userDetail.getRoles();

        assertThat(roles, hasSize(7));
        assertThat(roles, hasItem("topic/read"));
        assertThat(roles, hasItem("registry/version/delete"));
        assertThat(roles, hasItem("topic/data/read"));

        List<String> topicsFilterList = (List) (userDetail.getAttributes("roles", "username").get("topicsFilterRegexp"));
        assertThat(topicsFilterList, hasSize(2));
        assertThat(topicsFilterList, hasItem("test.*"));
        assertThat(topicsFilterList, hasItem("test-operator.*"));
    }

    @Test
    public void successWithOidcGroupAndUserRole() {

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

        assertThat(response, instanceOf(UserDetails.class));

        UserDetails userDetail = (UserDetails) response;

        assertTrue(userDetail.isAuthenticated());
        assertEquals("user2", userDetail.getUsername());

        Collection<String> roles = userDetail.getRoles();

        assertThat(roles, hasSize(7));
        assertThat(roles, hasItem("topic/read"));
        assertThat(roles, hasItem("registry/version/delete"));
        assertThat(roles, hasItem("topic/data/read"));

        List<String> topicsFilterList = (List) (userDetail.getAttributes("roles", "username").get("topicsFilterRegexp"));
        assertThat(topicsFilterList, hasSize(2));
        assertThat(topicsFilterList, hasItem("test.*"));
        assertThat(topicsFilterList, hasItem("test-operator.*"));
    }

    @Test
    public void successWithoutRoles() {

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

        assertThat(response, instanceOf(UserDetails.class));

        UserDetails userDetail = (UserDetails) response;

        assertTrue(userDetail.isAuthenticated());
        assertEquals("user", userDetail.getUsername());

        Collection<String> roles = userDetail.getRoles();
        assertThat(roles, hasSize(0));
    }

    @Test
    public void failure() {

        Mockito.when(tokenEndpointClient.sendRequest(ArgumentMatchers.any()))
                .thenReturn(Publishers.just(new OpenIdTokenResponse()));
        Mockito.when(openIdTokenResponseValidator.validate(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(Optional.empty());

        AuthenticationException authenticationException = assertThrows(AuthenticationException.class, () -> {
            Flowable
                    .fromPublisher(oidcProvider.authenticate(null, new UsernamePasswordCredentials(
                            "user",
                            "pass"
                    ))).blockingFirst();
        });

        assertThat(authenticationException.getResponse(), instanceOf(AuthenticationFailed.class));
        assertFalse(authenticationException.getResponse().isAuthenticated());
    }

    @Test
    void noLoginForm(){
        AkhqController.AuthDefinition actual = akhqController.auths();

        assertTrue(actual.isLoginEnabled(), "Login must be enabled with OIDC");
        assertFalse(actual.isFormEnabled(), "Login Form must not be active if only OIDC is enabled");
        assertFalse(actual.getOidcAuths().isEmpty());
    }
}
