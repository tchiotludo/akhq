package org.akhq.mcp;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import jakarta.inject.Singleton;

/**
 * Single source of truth for the path of the MCP endpoint.
 * <p>
 * The path is derived from the MCP server configuration and the server context path, so that the components
 * protecting the endpoint cannot drift from the route actually served by the MCP server.
 */
@Singleton
public class McpEndpoint {
    private final String contextPath;
    private final String endpoint;
    private final String path;

    public McpEndpoint(
        @Value("${micronaut.server.context-path:}") String contextPath,
        @Value("${micronaut.mcp.server.endpoint:/mcp}") String endpoint
    ) {
        this.contextPath = normalize(contextPath);
        this.endpoint = normalize(endpoint);
        String fullPath = this.contextPath + this.endpoint;
        this.path = fullPath.isEmpty() ? "/" : fullPath;
    }

    /**
     * @return the context path, without trailing slash, or an empty string when AKHQ is served at the root
     */
    public String contextPath() {
        return contextPath;
    }

    /**
     * @return the MCP endpoint relative to the context path, such as {@code /mcp}
     */
    public String endpoint() {
        return endpoint;
    }

    /**
     * @return the full request path of the MCP endpoint, context path included
     */
    public String path() {
        return path;
    }

    public boolean matches(HttpRequest<?> request) {
        String requestPath = request.getPath();
        String prefix = path.endsWith("/") ? path : path + "/";
        return requestPath.equals(path) || requestPath.startsWith(prefix);
    }

    private static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }

        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
