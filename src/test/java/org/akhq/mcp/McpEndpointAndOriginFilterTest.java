package org.akhq.mcp;

import io.micronaut.http.HttpRequest;
import org.akhq.configs.Mcp;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpEndpointAndOriginFilterTest {
    @Test
    void matchesTheEndpointAndItsSubPathsOnly() {
        McpEndpoint endpoint = new McpEndpoint("", "/mcp");

        assertTrue(endpoint.matches(HttpRequest.POST("/mcp", "")));
        assertTrue(endpoint.matches(HttpRequest.POST("/mcp/sub", "")));
        assertFalse(endpoint.matches(HttpRequest.POST("/mcpx", "")));
        assertFalse(endpoint.matches(HttpRequest.GET("/api/topics")));
    }

    @Test
    void prefixesTheContextPath() {
        McpEndpoint endpoint = new McpEndpoint("/akhq/", "mcp/");

        assertEquals("/akhq", endpoint.contextPath());
        assertEquals("/mcp", endpoint.endpoint());
        assertEquals("/akhq/mcp", endpoint.path());
        assertTrue(endpoint.matches(HttpRequest.POST("/akhq/mcp", "")));
        assertFalse(endpoint.matches(HttpRequest.POST("/mcp", "")));
    }

    @Test
    void acceptsRequestsWithoutOriginSentByNonBrowserClients() {
        assertTrue(filter(List.of()).isAllowed(HttpRequest.POST("/mcp", "")));
    }

    @Test
    void rejectsBrowserRequestsFromUnlistedOrigins() {
        assertFalse(filter(List.of()).isAllowed(HttpRequest.POST("/mcp", "").header("Origin", "https://evil.example.com")));
        assertFalse(filter(List.of()).isAllowed(HttpRequest.POST("/mcp", "").header("Origin", "null")));
    }

    @Test
    void acceptsListedOriginsRegardlessOfCaseAndTrailingSlash() {
        McpOriginFilter filter = filter(List.of("https://Inspector.example.com/"));

        assertTrue(filter.isAllowed(HttpRequest.POST("/mcp", "").header("Origin", "https://inspector.example.com")));
    }

    @Test
    void ignoresRequestsOutsideTheMcpEndpoint() {
        assertTrue(filter(List.of()).isAllowed(HttpRequest.GET("/api/topics").header("Origin", "https://evil.example.com")));
    }

    private McpOriginFilter filter(List<String> allowedOrigins) {
        Mcp mcp = new Mcp();
        mcp.setAllowedOrigins(allowedOrigins);
        return new McpOriginFilter(new McpEndpoint("", "/mcp"), mcp);
    }
}
