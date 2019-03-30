package org.kafkahq.modules;

import io.micronaut.security.authentication.*;
import io.reactivex.Flowable;
import org.kafkahq.configs.BasicAuth;
import org.reactivestreams.Publisher;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class BasicAuthAuthenticationProvider implements AuthenticationProvider {
    @Inject
    private List<BasicAuth> auths;

    @Override
    public Publisher<AuthenticationResponse> authenticate(AuthenticationRequest authenticationRequest) {
        for(BasicAuth auth : auths) {
            if (authenticationRequest.getIdentity().equals(auth.getUsername()) &&
                auth.isValidPassword((String) authenticationRequest.getSecret())) {
                UserDetails userDetails = new UserDetails(auth.getUsername(), auth.getRoles());

                return Flowable.just(userDetails);
            }
        }

        return Flowable.just(new AuthenticationFailed());
    }
}