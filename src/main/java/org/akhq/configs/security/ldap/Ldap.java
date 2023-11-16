package org.akhq.configs.security.ldap;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("akhq.security.ldap")
@Data
@Serdeable
public class Ldap {
    private String defaultGroup;
    private List<GroupMapping> groups = new ArrayList<>();
    private List<UserMapping> users = new ArrayList<>();
}
