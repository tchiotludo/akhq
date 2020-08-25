package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.StringUtils;
import lombok.Data;
import org.akhq.utils.UserGroupUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("akhq.security.ldap")
@Data
public class Ldap {
    private String defaultGroup;
    private Map<String, GroupMapping> groups = new HashMap<>();
    private Map<String, UserMapping> users = new HashMap<>();
    /**
     * @deprecated Naming isn't consistent with the other mappings
     */
    @Deprecated(forRemoval = true)
    private Map<String, GroupMapping> group = new HashMap<>();
    /**
     * @deprecated Naming isn't consistent with the other mappings
     */
    @Deprecated(forRemoval = true)
    private Map<String, UserMapping> user = new HashMap<>();

    public List<GroupMapping> getGroups() {
        List<GroupMapping> merged = new ArrayList<>();
        merged.addAll(groups.values());
        merged.addAll(group.values());
        return merged;
    }

    public List<UserMapping> getUsers() {
        List<UserMapping> merged = new ArrayList<>();
        merged.addAll(users.values());
        merged.addAll(user.values());
        return merged;
    }

    public boolean isEnabled() {
        return StringUtils.hasText(defaultGroup) || !getGroups().isEmpty() || !getUsers().isEmpty();
    }

    @PostConstruct
    public void postConstruct() {
        UserGroupUtils.finalizeGroupMappings(groups);
        UserGroupUtils.finalizeGroupMappings(group);
        UserGroupUtils.finalizeUserMappings(users);
        UserGroupUtils.finalizeUserMappings(user);
    }
}
