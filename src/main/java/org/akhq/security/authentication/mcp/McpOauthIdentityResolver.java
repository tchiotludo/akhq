package org.akhq.security.authentication.mcp;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.inject.Singleton;
import org.akhq.configs.security.McpOauth;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class McpOauthIdentityResolver {
    private final McpOauth mcpOauth;

    public McpOauthIdentityResolver(McpOauth mcpOauth) {
        this.mcpOauth = mcpOauth;
    }

    public String username(JWTClaimsSet claims) throws ParseException {
        return username(claims.getClaims(), claims.getSubject());
    }

    public String username(Map<String, Object> claims, String fallback) {
        return Optional.ofNullable(claimValue(claims, mcpOauth.getUsernameClaim()))
            .map(String::valueOf)
            .filter(value -> !value.isBlank())
            .orElse(fallback);
    }

    public List<String> groups(Map<String, Object> claims) {
        Object groups = claimValue(claims, mcpOauth.getGroupsClaim());
        if (groups instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toList();
        }
        if (groups instanceof String value && !value.isBlank()) {
            return Arrays.stream(value.split("\\s+")).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Object claimValue(Map<String, Object> claims, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        // Namespaced claims such as `https://acme.com/groups` contain dots and are not a nested path.
        if (claims.containsKey(path)) {
            return claims.get(path);
        }

        Object value = claims;
        for (String field : path.split("\\.")) {
            if (!(value instanceof Map<?, ?> map)) {
                return null;
            }
            value = ((Map<String, Object>) map).get(field);
        }
        return value;
    }
}
