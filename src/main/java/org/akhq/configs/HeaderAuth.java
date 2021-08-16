package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.security.config.SecurityConfigurationProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@ConfigurationProperties("akhq.security.header-auth")
public class HeaderAuth {
    String userHeader;
    String groupsHeader;
    String groupsHeaderSeparator = ",";
    List<Users> users;
    List<String> ipPatterns = Collections.singletonList(SecurityConfigurationProperties.ANYWHERE);

    @Data
    public static class Users {
        String username;
        List<String> groups = new ArrayList<>();
    }
}
