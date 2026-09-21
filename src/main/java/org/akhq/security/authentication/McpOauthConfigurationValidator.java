package org.akhq.security.authentication;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import jakarta.annotation.PostConstruct;
import org.akhq.configs.security.McpOauth;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Fails the application startup when the MCP OAuth configuration is incomplete, instead of rejecting every MCP
 * request at runtime.
 */
@Context
@Requires(property = "akhq.security.mcp-oauth.enabled", value = "true")
public class McpOauthConfigurationValidator {
    private final McpOauth mcpOauth;

    public McpOauthConfigurationValidator(McpOauth mcpOauth) {
        this.mcpOauth = mcpOauth;
    }

    @PostConstruct
    void validate() {
        List<String> errors = new ArrayList<>();

        requireText(errors, mcpOauth.getIssuer(), "issuer");
        requireText(errors, mcpOauth.getJwksUrl(), "jwks-url");
        requireText(errors, mcpOauth.getAudience(), "audience");
        requireText(errors, mcpOauth.getAuthorizationServer(), "authorization-server");
        requireUri(errors, mcpOauth.getJwksUrl(), "jwks-url");
        requireUri(errors, mcpOauth.getResource(), "resource");

        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                "Invalid `akhq.security.mcp-oauth` configuration: " + String.join(", ", errors)
            );
        }
    }

    private void requireText(List<String> errors, String value, String property) {
        if (value == null || value.isBlank()) {
            errors.add("`" + property + "` is required");
        }
    }

    private void requireUri(List<String> errors, String value, String property) {
        if (value == null || value.isBlank()) {
            return;
        }

        try {
            if (!new URI(value).isAbsolute()) {
                errors.add("`" + property + "` must be an absolute URL");
            }
        } catch (URISyntaxException e) {
            errors.add("`" + property + "` is not a valid URL");
        }
    }
}
