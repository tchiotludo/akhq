package org.akhq.security.authentication;

import io.micronaut.http.HttpRequest;
import jakarta.inject.Singleton;
import org.akhq.configs.security.McpOauth;

@Singleton
public class McpOauthRequestMatcher {
    private final McpOauth mcpOauth;

    public McpOauthRequestMatcher(McpOauth mcpOauth) {
        this.mcpOauth = mcpOauth;
    }

    public boolean matches(HttpRequest<?> request) {
        String endpoint = mcpOauth.getEndpoint();
        return mcpOauth.isEnabled()
            && (request.getPath().equals(endpoint) || request.getPath().startsWith(endpoint + "/"));
    }
}
