package org.akhq.security.authentication.mcp;

import io.micronaut.http.HttpRequest;
import jakarta.inject.Singleton;
import org.akhq.configs.security.McpOauth;
import org.akhq.mcp.McpEndpoint;

@Singleton
public class McpOauthRequestMatcher {
    private final McpOauth mcpOauth;
    private final McpEndpoint mcpEndpoint;

    public McpOauthRequestMatcher(McpOauth mcpOauth, McpEndpoint mcpEndpoint) {
        this.mcpOauth = mcpOauth;
        this.mcpEndpoint = mcpEndpoint;
    }

    public boolean matches(HttpRequest<?> request) {
        return mcpOauth.isEnabled() && mcpEndpoint.matches(request);
    }
}
