package org.akhq.configs;

import com.google.common.hash.Hashing;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Data
public class BasicAuth {
    String username;
    String password;
    List<String> groups = new ArrayList<>();

    @SuppressWarnings("UnstableApiUsage")
    public boolean isValidPassword(String password) {
        return this.password.equals(
                Hashing.sha256()
                        .hashString(password, StandardCharsets.UTF_8)
                        .toString()
        );
    }
}

