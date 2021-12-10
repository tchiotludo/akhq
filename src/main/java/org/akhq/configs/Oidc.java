package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("akhq.security.oidc")
@Data
public class Oidc {
    private boolean enabled;
    private Map<String, Provider> providers;

    @Data
    public static class Provider {
        private String label = "Login with OIDC";
        private String usernameField = OpenIdClaims.CLAIMS_PREFERRED_USERNAME;
        private String groupsField = "roles";
        private String defaultGroup;
        private List<GroupMapping> groups = new ArrayList<>();
        private List<UserMapping> users = new ArrayList<>();
        private boolean useOidcClaim = false;
    }

    public Provider getProvider(String key) {
        providers.putIfAbsent(key, new Provider());
        return providers.get(key);
    }
}
