package org.akhq.modules;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.*;
import io.reactivex.Flowable;
import org.akhq.configs.BasicAuth;
import org.akhq.configs.Ldap;
import org.akhq.configs.Oidc;
import org.akhq.configs.SecurityProperties;
import org.akhq.utils.ClaimProvider;
import org.akhq.utils.UserGroupUtils;
import org.reactivestreams.Publisher;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class BasicAuthAuthenticationProvider implements AuthenticationProvider {
    @Inject
    private SecurityProperties securityProperties;
    @Inject
    private ClaimProvider claimProvider;

    @Override
    public Publisher<AuthenticationResponse> authenticate(@Nullable HttpRequest<?> httpRequest, AuthenticationRequest<?, ?> authenticationRequest) {
        String username = String.valueOf(authenticationRequest.getIdentity());
        for (BasicAuth auth : securityProperties.getBasicAuth()) {
            if (!username.equals(auth.getUsername())) {
                continue;
            }
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

            ClaimProvider.AKHQClaimResponse claim = claimProvider.generateClaim(request);
            UserDetails userDetails = new UserDetails(
                    auth.getUsername(),
                    claim.getRoles(),
                    claim.getAttributes());
            return Flowable.just(userDetails);
        }

        return Flowable.just(new AuthenticationFailed(AuthenticationFailureReason.USER_NOT_FOUND));
    }
}
