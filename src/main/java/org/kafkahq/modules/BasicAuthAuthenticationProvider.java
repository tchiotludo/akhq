package org.kafkahq.modules;

import io.micronaut.core.util.StringUtils;
import io.micronaut.security.authentication.*;
import io.reactivex.Flowable;
import org.kafkahq.configs.BasicAuth;
import org.kafkahq.configs.SecurityGroup;
import org.reactivestreams.Publisher;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class BasicAuthAuthenticationProvider implements AuthenticationProvider {
    @Inject
    private List<BasicAuth> auths;
    @Inject
    private List<SecurityGroup> groups;

    @Override
    public Publisher<AuthenticationResponse> authenticate(AuthenticationRequest authenticationRequest) {
        for(BasicAuth auth : auths) {
            if (authenticationRequest.getIdentity().equals(auth.getUsername()) &&
                auth.isValidPassword((String) authenticationRequest.getSecret())) {

                UserDetails userDetails = new UserDetails(auth.getUsername(), getUserRoles(auth), auth.getAttributes());

                return Flowable.just(userDetails);
            }
        }

        return Flowable.just(new AuthenticationFailed());
    }

    private List<String> getUserRoles(BasicAuth auth) {
        Set<String> roles = auth.getRoles();
        if(roles == null) {
            roles = new HashSet<>();
        }

        if(StringUtils.isNotEmpty(auth.getGroup())) {
            roles.addAll(groups.stream().filter(securityGroup -> securityGroup.getName().equals(auth.getGroup())).findAny()
                    .map(securityGroup -> securityGroup.getRoles()).orElseGet(HashSet::new));
        }
        return new ArrayList<>(roles);
    }
}
