package org.akhq.security.authentication;

import io.micronaut.http.HttpRequest;
import jakarta.inject.Singleton;
import org.akhq.configs.security.McpOauth;

import java.net.URI;

@Singleton
public class McpOauthResourceMetadata {
    private final McpOauth mcpOauth;

    public McpOauthResourceMetadata(McpOauth mcpOauth) {
        this.mcpOauth = mcpOauth;
    }

    public String challenge(HttpRequest<?> request) {
        String challenge = "Bearer resource_metadata=\"" + metadataUri(request) + "\"";
        if (mcpOauth.getRequiredScope() != null && !mcpOauth.getRequiredScope().isBlank()) {
            challenge += ", scope=\"" + mcpOauth.getRequiredScope() + "\"";
        }
        return challenge;
    }

    public String resource(HttpRequest<?> request) {
        return mcpOauth.getResource() == null || mcpOauth.getResource().isBlank()
            ? request.getUri().resolve(mcpOauth.getEndpoint()).toString()
            : mcpOauth.getResource();
    }

    private URI metadataUri(HttpRequest<?> request) {
        return URI.create(resource(request)).resolve("/.well-known/oauth-protected-resource");
    }
}
