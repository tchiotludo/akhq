package org.akhq.security.authentication.mcp;

import io.micronaut.http.HttpRequest;
import org.akhq.configs.security.McpOauth;
import org.akhq.mcp.McpEndpoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpOauthProtectedResourceMetadataProviderTest {
    // The Netty server exposes the request target as a relative URI, so the origin must come from the host resolver.
    private static final HttpRequest<?> NETTY_LIKE_REQUEST = HttpRequest.POST("/mcp", "");

    @Test
    void returnsProtectedResourceMetadata() {
        McpOauth configuration = configuration();

        var metadata = new McpOauthProtectedResourceMetadataProvider(configuration, resourceMetadata(configuration, ""))
            .get(HttpRequest.GET("/.well-known/oauth-protected-resource/mcp"));

        assertEquals("https://akhq.example.com/mcp", metadata.resource());
        assertEquals(List.of("https://identity.example.com"), metadata.authorizationServers());
        assertEquals(List.of("header"), metadata.bearerMethodsSupported());
        assertEquals(List.of("akhq.mcp.read"), metadata.scopesSupported());
    }

    @Test
    void advertisesAbsoluteUrlsForRelativeRequestUris() {
        McpOauthResourceMetadata resourceMetadata = resourceMetadata(configuration(), "");

        assertEquals("https://akhq.example.com/mcp", resourceMetadata.resource(NETTY_LIKE_REQUEST));
        assertEquals(
            "https://akhq.example.com/.well-known/oauth-protected-resource/mcp",
            resourceMetadata.metadataUri(NETTY_LIKE_REQUEST)
        );
        assertEquals(
            "Bearer resource_metadata=\"https://akhq.example.com/.well-known/oauth-protected-resource/mcp\", scope=\"akhq.mcp.read\"",
            resourceMetadata.challenge(NETTY_LIKE_REQUEST)
        );
    }

    @Test
    void includesTheContextPath() {
        McpOauthResourceMetadata resourceMetadata = resourceMetadata(configuration(), "/akhq/");

        assertEquals("https://akhq.example.com/akhq/mcp", resourceMetadata.resource(NETTY_LIKE_REQUEST));
        assertEquals(
            "https://akhq.example.com/akhq/.well-known/oauth-protected-resource/mcp",
            resourceMetadata.metadataUri(NETTY_LIKE_REQUEST)
        );
    }

    @Test
    void usesTheConfiguredResourceOrigin() {
        McpOauth configuration = configuration();
        configuration.setResource("https://public.example.com:8443/mcp");
        McpOauthResourceMetadata resourceMetadata = resourceMetadata(configuration, "");

        assertEquals("https://public.example.com:8443/mcp", resourceMetadata.resource(NETTY_LIKE_REQUEST));
        assertEquals(
            "https://public.example.com:8443/.well-known/oauth-protected-resource/mcp",
            resourceMetadata.metadataUri(NETTY_LIKE_REQUEST)
        );
    }

    private McpOauth configuration() {
        McpOauth configuration = new McpOauth();
        configuration.setAuthorizationServer("https://identity.example.com");
        configuration.setRequiredScope("akhq.mcp.read");
        return configuration;
    }

    private McpOauthResourceMetadata resourceMetadata(McpOauth configuration, String contextPath) {
        return new McpOauthResourceMetadata(
            configuration,
            new McpEndpoint(contextPath, "/mcp"),
            request -> "https://akhq.example.com"
        );
    }
}
