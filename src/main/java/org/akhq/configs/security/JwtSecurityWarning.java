package org.akhq.configs.security;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Value;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.lang.Runnable;

@Singleton
@Slf4j
@Context
public class JwtSecurityWarning {
    protected static String DEFAULT = "pleasechangeme!pleasechangeme!pleasechangeme!pleasechangeme!" +
        "pleasechangeme!pleasechangeme!pleasechangeme!pleasechangeme!pleasechangeme!pleasechangeme!" +
        "pleasechangeme!pleasechangeme!pleasechangeme!pleasechangeme!pleasechangeme!pleasechangeme!";

    @Value("${micronaut.security.token.jwt.signatures.secret.generator.secret}")
    protected String secret;

    @Value("${micronaut.security.enabled:false}")
    protected Boolean enabled;

    @Inject
    protected SecurityProperties securityProperties;

    @PostConstruct
    public void start() {
        if (enabled && secret.equals(DEFAULT)) {
            logSecurityWarning(() -> {
                log.warn("You still use the default jwt secret.");
                log.warn("This known secret can be used to impersonate anyone.");
                log.warn("Please change 'micronaut.security.token.jwt.signatures.secret.generator.secret' configuration, or ask your administrator to do it !");
            });
        } else if (!enabled && securityProperties.getGroups() != null && !securityProperties.getGroups().isEmpty()){
            logSecurityWarning(() -> {
                log.warn("You have set a security group config but did not set the jwt secret.");
                log.warn("This means that the API request will not be checked against the security group config.");
                log.warn("Please set the 'micronaut.security.token.jwt.signatures.secret.generator.secret' configuration, or ask your administrator to do it !");
            });
        }
    }

    private static void logSecurityWarning(Runnable printBody) {
        log.warn("");
        log.warn("##############################################################");
        log.warn("#                      SECURITY WARNING                      #");
        log.warn("##############################################################");
        log.warn("");
        printBody.run();
        log.warn("");
        log.warn("##############################################################");
        log.warn("");
    }
}
