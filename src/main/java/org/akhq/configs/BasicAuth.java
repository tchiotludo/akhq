package org.akhq.configs;

import com.google.common.hash.Hashing;
import lombok.Data;
import org.mindrot.jbcrypt.BCrypt;

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
        if (this.password.startsWith("$2")) {
            // See http://www.mindrot.org/projects/jBCrypt/
            return BCrypt.checkpw(password, this.password);
        } else {
            // Default Sha256 format
            return this.password.equals(
                    Hashing.sha256()
                            .hashString(password, StandardCharsets.UTF_8)
                            .toString()
            );
        }
    }

}

