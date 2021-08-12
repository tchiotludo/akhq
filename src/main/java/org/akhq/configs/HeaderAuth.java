package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties("akhq.security.header-auth")
public class HeaderAuth {
    String userHeader;
    String groupsHeader;
    List<Users> users;

    @Data
    public static class Users {
        String username;
        List<String> groups = new ArrayList<>();
    }
}

