package org.akhq.modules;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.*;
import io.reactivex.Flowable;
import org.akhq.configs.BasicAuth;
import org.akhq.configs.SecurityProperties;
import org.akhq.utils.ClaimProvider;
import org.reactivestreams.Publisher;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class BasicAuthAuthenticationProvider implements AuthenticationProvider {
    @Inject
    private SecurityProperties securityProperties;
    @Inject
    private ClaimProvider claimProvider;

    @Override
    public Publisher<AuthenticationResponse> authenticate(@Nullable HttpRequest<?> httpRequest, AuthenticationRequest<?, ?> authenticationRequest) {
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

        ClaimProvider.AKHQClaimRequest request =
                ClaimProvider.AKHQClaimRequest.builder()
                        .providerType(ClaimProvider.ProviderType.BASIC_AUTH)
                        .providerName(null)
                        .username(auth.getUsername())
                        .groups(auth.getGroups())
                        .build();
        try {
            ClaimProvider.AKHQClaimResponse claim = claimProvider.generateClaim(request);
            return Flowable.just(new UserDetails(auth.getUsername(), claim.getRoles(), claim.getAttributes()));
        } catch (Exception e) {
            String claimProviderClass = claimProvider.getClass().getName();
            return Flowable.just(new AuthenticationFailed("Exception from ClaimProvider " + claimProviderClass + ": " + e.getMessage()));
        }
    }
}
