package org.akhq.modules;

import edu.umd.cs.findbugs.annotations.Nullable;
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
        for(BasicAuth auth : securityProperties.getBasicAuth()) {
            if (authenticationRequest.getIdentity().equals(auth.getUsername()) &&
                auth.isValidPassword((String) authenticationRequest.getSecret())) {

                UserDetails userDetails = new UserDetails(auth.getUsername(),
                        userGroupUtils.getUserRoles(auth.getGroups()),
                        userGroupUtils.getUserAttributes(auth.getGroups()));

                return Flowable.just(userDetails);
            }
        }

        return Flowable.just(new AuthenticationFailed());
    }
}
