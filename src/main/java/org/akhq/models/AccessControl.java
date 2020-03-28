package org.akhq.models;

import lombok.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class AccessControl {
    private String principal;
    private String encodedPrincipal;
    private final Map<String, Map<HostResource, List<String>>> permissions = new HashMap<>();

    public static String encodePrincipal(String principal) {
        return Base64.getEncoder().encodeToString(principal.getBytes());
    }

    public static String decodePrincipal(String encodedPrincipal) {
        return new String(Base64.getDecoder().decode(encodedPrincipal));
    }

    public AccessControl(String principal) {
        this.principal = principal;
        this.encodedPrincipal = encodePrincipal(principal);
    }

    public AccessControl(String principal, HashMap<String, Map<HostResource, List<String>>> permissions) {
        this(principal);
        this.permissions.putAll(permissions);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HostResource {
        private String host;
        private String resource;
    }
}
