package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("akhq.security.oauth2")
@Data
public class Oauth {
    private boolean enabled;
    private Map<String, Provider> providers;

    @Data
    public static class Provider {
        private String label = "Login with OAuth";
        private String usernameField = "login";
        private String groupsField = "organizations_url";
        private String defaultGroup;
        private List<GroupMapping> groups = new ArrayList<>();
        private List<UserMapping> users = new ArrayList<>();
    }

    public Provider getProvider(String key) {
        providers.putIfAbsent(key, new Provider());
        return providers.get(key);
    }
}
