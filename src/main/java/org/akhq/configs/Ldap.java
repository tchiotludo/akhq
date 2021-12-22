package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("akhq.security.ldap")
@Data
public class Ldap {
    private List<String> defaultBindings;
    private List<GroupMapping> groups = new ArrayList<>();
    private List<UserMapping> users = new ArrayList<>();
}
