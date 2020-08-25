package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims;
import lombok.Data;
import org.akhq.utils.UserGroupUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
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
        private Map<String, GroupMapping> groups = new HashMap<>();
        private Map<String, UserMapping> users = new HashMap<>();

        public List<GroupMapping> getGroups() {
            return new ArrayList<>(groups.values());
        }

        public List<UserMapping> getUsers() {
            return new ArrayList<>(users.values());
        }

        @PostConstruct
        public void postConstruct() {
            UserGroupUtils.finalizeGroupMappings(groups);
            UserGroupUtils.finalizeUserMappings(users);
        }
    }

    public Provider getProvider(String key) {
        providers.putIfAbsent(key, new Provider());
        return providers.get(key);
    }
}
