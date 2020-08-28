package org.akhq.modules;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.*;
import io.reactivex.Flowable;
import org.akhq.configs.BasicAuth;
import org.akhq.utils.UserGroupUtils;
import org.reactivestreams.Publisher;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class BasicAuthAuthenticationProvider implements AuthenticationProvider {
    @Inject
    private List<BasicAuth> auths;

    @Inject
    private UserGroupUtils userGroupUtils;

    @Override
    public Publisher<AuthenticationResponse> authenticate(@Nullable HttpRequest<?> httpRequest, AuthenticationRequest<?, ?> authenticationRequest) {
        for(BasicAuth auth : auths) {
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
