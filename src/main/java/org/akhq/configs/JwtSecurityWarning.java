package org.akhq.configs;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Value;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

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

    @PostConstruct
    public void start() {
        if (enabled && secret.equals(DEFAULT)) {
            log.warn("");
            log.warn("##############################################################");
            log.warn("#                      SECURITY WARNING                      #");
            log.warn("##############################################################");
            log.warn("");
            log.warn("You still use the default jwt secret.");
            log.warn("This known secret can be used to impersonate anyone.");
            log.warn("Please change 'micronaut.security.token.jwt.signatures.secret.generator.secret' configuration, or ask your administrator to do it !");
            log.warn("");
            log.warn("##############################################################");
            log.warn("");
        }
    }
}
