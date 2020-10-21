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
    PasswordHash passwordHash = PasswordHash.SHA256;
    List<String> groups = new ArrayList<>();

    @SuppressWarnings("UnstableApiUsage")
    public boolean isValidPassword(String password) {
        return passwordHash.checkPassword(password, this.password);
    }

    /**
     * Password hashing algorithm
     */
    public enum PasswordHash {
        SHA256 {
            @Override
            public boolean checkPassword(String plainPassword, String hashedPassword) {
                return hashedPassword.equals(
                        Hashing.sha256()
                                .hashString(plainPassword, StandardCharsets.UTF_8)
                                .toString());
            }
        },
        BCRYPT {
            @Override
            public boolean checkPassword(String plainPassword, String hashedPassword) {
                // See http://www.mindrot.org/projects/jBCrypt/
                return BCrypt.checkpw(plainPassword, hashedPassword);
            }
        };

        public abstract boolean checkPassword(String plainPassword, String hashedPassword);
    }
}

