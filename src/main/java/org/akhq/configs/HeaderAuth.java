package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties("akhq.security.header-auth")
public class HeaderAuth {
    String userHeader;
    String groupsHeader;
    String groupsHeaderSeparator = ",";
    List<Users> users;

    @Data
    public static class Users {
        String username;
        List<String> groups = new ArrayList<>();
    }
}
