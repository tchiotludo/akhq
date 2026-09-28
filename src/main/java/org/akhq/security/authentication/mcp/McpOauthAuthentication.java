package org.akhq.security.authentication.mcp;

import io.micronaut.security.authentication.ServerAuthentication;

import java.util.List;
import java.util.Map;

/**
 * Authentication issued from a validated MCP OAuth access token.
 * <p>
 * The dedicated type lets authorization components resolve groups from the external identity provider claims
 * instead of the AKHQ-issued UI cookie JWT format.
 */
public class McpOauthAuthentication extends ServerAuthentication {
    public McpOauthAuthentication(String name, Map<String, Object> attributes) {
        super(name, List.of(), attributes);
    }
}
