package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims;
import lombok.Data;

@ConfigurationProperties("akhq.security.oidc")
@Data
public class Oidc {
    private boolean enabled;
    private String provider = "default";
    private String label = "Login with OIDC";
    private String usernameField = OpenIdClaims.CLAIMS_PREFERRED_USERNAME;
    private String rolesField = "roles";
}
