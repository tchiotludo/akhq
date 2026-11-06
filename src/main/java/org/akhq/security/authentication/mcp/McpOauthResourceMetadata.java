package org.akhq.security.authentication.mcp;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.server.util.HttpHostResolver;
import jakarta.inject.Singleton;
import org.akhq.configs.security.McpOauth;
import org.akhq.mcp.McpEndpoint;

import java.net.URI;

/**
 * Builds the absolute URLs advertised to MCP clients for OAuth discovery (RFC 9728).
 * <p>
 * Both URLs must be absolute: MCP clients resolve them outside of any request context, and a relative
 * {@code resource_metadata} makes discovery fail.
 */
@Singleton
public class McpOauthResourceMetadata {
    static final String WELL_KNOWN_PATH = "/.well-known/oauth-protected-resource";

    private final McpOauth mcpOauth;
    private final McpEndpoint mcpEndpoint;
    private final HttpHostResolver httpHostResolver;

    public McpOauthResourceMetadata(McpOauth mcpOauth, McpEndpoint mcpEndpoint, HttpHostResolver httpHostResolver) {
        this.mcpOauth = mcpOauth;
        this.mcpEndpoint = mcpEndpoint;
        this.httpHostResolver = httpHostResolver;
    }

    public String challenge(HttpRequest<?> request) {
        String challenge = "Bearer resource_metadata=\"" + metadataUri(request) + "\"";
        if (mcpOauth.getRequiredScope() != null && !mcpOauth.getRequiredScope().isBlank()) {
            challenge += ", scope=\"" + mcpOauth.getRequiredScope() + "\"";
        }
        return challenge;
    }

    /**
     * @return the configured {@code resource}, or the public origin of AKHQ followed by the MCP endpoint path
     */
    public String resource(HttpRequest<?> request) {
        return hasConfiguredResource()
            ? mcpOauth.getResource()
            : origin(request) + mcpEndpoint.path();
    }

    /**
     * @return the URL of the protected resource metadata, following the RFC 9728 layout where the resource path is
     * appended to the well-known path. Micronaut serves this document below the context path.
     */
    public String metadataUri(HttpRequest<?> request) {
        return origin(request) + mcpEndpoint.contextPath() + WELL_KNOWN_PATH + mcpEndpoint.endpoint();
    }

    private String origin(HttpRequest<?> request) {
        if (hasConfiguredResource()) {
            URI resource = URI.create(mcpOauth.getResource());
            return resource.getScheme() + "://" + resource.getRawAuthority();
        }

        return httpHostResolver.resolve(request).replaceAll("/+$", "");
    }

    private boolean hasConfiguredResource() {
        return mcpOauth.getResource() != null && !mcpOauth.getResource().isBlank();
    }
}
