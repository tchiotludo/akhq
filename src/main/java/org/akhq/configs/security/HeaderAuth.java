package org.akhq.configs.security;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.security.config.SecurityConfigurationProperties;
import lombok.Data;
import org.akhq.configs.security.ldap.GroupMapping;
import org.akhq.configs.security.ldap.UserMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@ConfigurationProperties("akhq.security.header-auth")
public class HeaderAuth {
    String userHeader;
    String groupsHeader;
    String groupsHeaderSeparator = ",";

    String defaultGroup;
    List<GroupMapping> groups = new ArrayList<>();
    List<UserMapping> users = new ArrayList<>();

    List<String> ipPatterns = Collections.singletonList(SecurityConfigurationProperties.ANYWHERE);
}
