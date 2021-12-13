package org.akhq.utils;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.AuthenticationFailed;
import io.micronaut.security.event.LoginFailedEvent;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Singleton;

@Singleton
@Slf4j
public class LoginFailedEventListener implements ApplicationEventListener<LoginFailedEvent> {
    @Override
    public void onApplicationEvent(LoginFailedEvent event) {
        if (event.getSource() instanceof AuthenticationFailed) {
            AuthenticationFailed authenticationFailed = (AuthenticationFailed) event.getSource();
            log.warn("Login failed reason {}, username {}, message {}",
                    authenticationFailed.getReason(),
                    authenticationFailed.getAuthentication().map(Authentication::getName).orElse("unknown"),
                    authenticationFailed.getMessage().orElse("none")
            );
        }
    }

    @Override
    public boolean supports(LoginFailedEvent event) {
        return true;
    }
}
