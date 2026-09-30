package org.akhq.configs.security;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;
import org.akhq.configs.security.ldap.GroupMapping;
import org.akhq.configs.security.ldap.UserMapping;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties("akhq.security.mcp-oauth")
public class McpOauth {
    private boolean enabled;
    private String authorizationServer;
    private String issuer;
    private String jwksUrl;
    private String audience;
    private String resource;
    private String usernameClaim = "preferred_username";
    private String groupsClaim = "groups";
    private String requiredScope;
    private String defaultGroup;
    private List<String> jwsAlgorithms = new ArrayList<>();
    private Duration jwksConnectTimeout = Duration.ofSeconds(5);
    private Duration jwksReadTimeout = Duration.ofSeconds(5);
    private List<GroupMapping> groups = new ArrayList<>();
    private List<UserMapping> users = new ArrayList<>();

    /**
     * @return the authorization server advertised in the protected resource metadata, defaulting to the issuer.
     */
    public String getAuthorizationServer() {
        return authorizationServer == null || authorizationServer.isBlank() ? issuer : authorizationServer;
    }
}
