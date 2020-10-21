package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.StringUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("akhq.security.ldap")
@Data
public class Ldap {
    private String defaultGroup;
    private List<GroupMapping> groups = new ArrayList<>();
    private List<UserMapping> users = new ArrayList<>();

    public boolean isEnabled() {
        return StringUtils.hasText(defaultGroup) || !getGroups().isEmpty() || !getUsers().isEmpty();
    }
}
