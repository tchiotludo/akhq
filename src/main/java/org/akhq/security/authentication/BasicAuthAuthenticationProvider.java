package org.akhq.security.authentication;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationFailed;
import io.micronaut.security.authentication.AuthenticationFailureReason;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.provider.HttpRequestReactiveAuthenticationProvider;
import io.micronaut.security.rules.SecurityRule;
import io.reactivex.Flowable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.akhq.configs.security.BasicAuth;
import org.akhq.configs.security.SecurityProperties;
import org.akhq.models.security.ClaimProvider;
import org.akhq.models.security.ClaimProviderType;
import org.akhq.models.security.ClaimRequest;
import org.akhq.models.security.ClaimResponse;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class BasicAuthAuthenticationProvider<B> implements HttpRequestReactiveAuthenticationProvider<B> {
    @Inject
    private SecurityProperties securityProperties;
    @Inject
    private ClaimProvider claimProvider;

    @Override
    public Publisher<AuthenticationResponse> authenticate(@Nullable HttpRequest<B> httpRequest,
                                                          AuthenticationRequest<String, String> authenticationRequest) {
        String username = String.valueOf(authenticationRequest.getIdentity());
        Optional<BasicAuth> optionalBasicAuth = securityProperties.getBasicAuth()
                .stream()
                .filter(basicAuth -> basicAuth.getUsername().equals(username))
                .findFirst();

        // User not found
        if(optionalBasicAuth.isEmpty()){
            return Flowable.just(new AuthenticationFailed(AuthenticationFailureReason.USER_NOT_FOUND));
        }
        BasicAuth auth = optionalBasicAuth.get();

        // Invalid password
        if (!auth.isValidPassword((String) authenticationRequest.getSecret())) {
            return Flowable.just(new AuthenticationFailed(AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH));
        }

        ClaimRequest request = ClaimRequest.builder()
            .providerType(ClaimProviderType.BASIC_AUTH)
            .providerName(null)
            .username(auth.getUsername())
            .groups(auth.getGroups())
            .build();

        try {
            ClaimResponse claim = claimProvider.generateClaim(request);
            return Flowable.just(AuthenticationResponse.success(auth.getUsername(), List.of(SecurityRule.IS_AUTHENTICATED), Map.of("groups", claim.getGroups())));
        } catch (Exception e) {
            String claimProviderClass = claimProvider.getClass().getName();
            return Flowable.just(new AuthenticationFailed("Exception from ClaimProvider " + claimProviderClass + ": " + e.getMessage()));
        }
    }
}
