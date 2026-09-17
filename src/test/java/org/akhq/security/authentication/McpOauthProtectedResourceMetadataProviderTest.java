package org.akhq.security.authentication;

import io.micronaut.http.HttpRequest;
import org.akhq.configs.security.McpOauth;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpOauthProtectedResourceMetadataProviderTest {
    @Test
    void returnsProtectedResourceMetadata() {
        McpOauth configuration = new McpOauth();
        configuration.setAuthorizationServer("https://identity.example.com");
        configuration.setRequiredScope("akhq.mcp.read");
        McpOauthResourceMetadata resourceMetadata = new McpOauthResourceMetadata(configuration);

        var metadata = new McpOauthProtectedResourceMetadataProvider(configuration, resourceMetadata)
            .get(HttpRequest.GET("https://akhq.example.com/.well-known/oauth-protected-resource"));

        assertEquals("https://akhq.example.com/mcp", metadata.resource());
        assertEquals(List.of("https://identity.example.com"), metadata.authorizationServers());
        assertEquals(List.of("header"), metadata.bearerMethodsSupported());
        assertEquals(List.of("akhq.mcp.read"), metadata.scopesSupported());
    }
}
