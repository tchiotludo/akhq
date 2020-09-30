package org.akhq.configs;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.Data;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class BasicAuth {
    String username;
    String password;
    List<String> groups = new ArrayList<>();

    /**
     * Crypt format
     * $<id>[$<param>=<value>(,<param>=<value>)*][$<salt>[$<hash>]]
     * https://en.wikipedia.org/wiki/Crypt_(C)
     */
    private static final Pattern CRYPT_FORMAT = Pattern.compile("^\\$([a-z0-9]+)\\$(.+)");

    @SuppressWarnings("UnstableApiUsage")
    public boolean isValidPassword(String password) {
        Matcher cryptMatcher = CRYPT_FORMAT.matcher(this.password);
        if (cryptMatcher.matches()) {
            // Crypt format
            String id = cryptMatcher.group(1);
            String hashPassword = cryptMatcher.group(2);
            if (id.startsWith("1")) {
                return checkPassword(password, Hashing.md5(), hashPassword);
            } else if (id.startsWith("2")) {
                // See http://www.mindrot.org/projects/jBCrypt/
                return BCrypt.checkpw(password, this.password);
            } else if (id.equals("5")) {
                return checkPassword(password, Hashing.sha256(), hashPassword);
            } else if (id.equals("6")) {
                return checkPassword(password, Hashing.sha512(), hashPassword);
            } else {
                throw new IllegalStateException("Unsupported crypt algorithm " + id);
            }

        } else {
            // Default Sha256 format
            return checkPassword(password, Hashing.sha256(), this.password);
        }
    }

    private static boolean checkPassword(String plainPassword, HashFunction hashFunction, String hashedPassword) {
        return hashedPassword.equals(
                hashFunction
                        .hashString(plainPassword, StandardCharsets.UTF_8)
                        .toString()
        );
    }
}

