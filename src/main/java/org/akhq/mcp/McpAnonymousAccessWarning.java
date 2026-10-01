package org.akhq.mcp;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Warns at startup when the MCP server is enabled while AKHQ security is disabled, since the MCP endpoint then
 * exposes topic data to anyone able to reach AKHQ. This is not fatal, as it is a legitimate setup for a local
 * instance, but it must never go unnoticed.
 */
@Slf4j
@Context
@Requires(property = "akhq.mcp.enabled", value = "true")
@Requires(property = "micronaut.security.enabled", value = "false")
class McpAnonymousAccessWarning {
    @PostConstruct
    void warn() {
        log.warn(
            "The MCP server is enabled while `micronaut.security.enabled` is false: the MCP endpoint is reachable "
                + "anonymously and exposes topic data to anyone able to reach AKHQ."
        );
    }
}
