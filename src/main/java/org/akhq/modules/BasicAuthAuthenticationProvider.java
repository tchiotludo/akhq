package org.akhq.modules;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.*;
import io.reactivex.Flowable;
import org.akhq.configs.BasicAuth;
import org.akhq.configs.Ldap;
import org.akhq.configs.Oidc;
import org.akhq.configs.SecurityProperties;
import org.akhq.utils.UserGroupUtils;
import org.reactivestreams.Publisher;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BasicAuthAuthenticationProvider implements AuthenticationProvider {
    @Inject
    private SecurityProperties securityProperties;
    @Inject
    private Oidc oidc;
    @Inject
    private Ldap ldap;

    @Inject
    private UserGroupUtils userGroupUtils;

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
            UserDetails userDetails = new UserDetails(username,
                    userGroupUtils.getUserRoles(auth.getGroups()),
                    userGroupUtils.getUserAttributes(auth.getGroups()));
            return Flowable.just(userDetails);
        }

        return Flowable.just(new AuthenticationFailed(AuthenticationFailureReason.USER_NOT_FOUND));
    }
}
