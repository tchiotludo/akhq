package org.akhq.configs;

import com.google.common.hash.Hashing;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.List;

@EachProperty("akhq.security.basic-auth")
@Getter
public class BasicAuth {
    String username;
    String password;
    List<String> groups;

    public BasicAuth(@Parameter String username) {
        this.username = username;
    }

    @SuppressWarnings("UnstableApiUsage")
    public boolean isValidPassword(String password) {
        return this.password.equals(
            Hashing.sha256()
            .hashString(password, StandardCharsets.UTF_8)
            .toString()
        );
    }
}

