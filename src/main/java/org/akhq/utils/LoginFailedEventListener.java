package org.akhq.utils;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.security.authentication.AuthenticationFailed;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.event.LoginFailedEvent;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;

@Singleton
@Slf4j
public class LoginFailedEventListener implements ApplicationEventListener<LoginFailedEvent> {
    @Override
    public void onApplicationEvent(LoginFailedEvent event) {
        if (event.getSource() instanceof AuthenticationFailed) {
            AuthenticationFailed authenticationFailed = (AuthenticationFailed) event.getSource();
            log.warn("Login failed reason {}, username {}, message {}",
                    authenticationFailed.getReason(),
                    authenticationFailed.getUserDetails().map(UserDetails::getUsername).orElse("unknown"),
                    authenticationFailed.getMessage().orElse("none")
            );
        }
    }

    @Override
    public boolean supports(LoginFailedEvent event) {
        return true;
    }
}
