package org.akhq.mcp;

import io.micronaut.core.order.Ordered;
import io.micronaut.core.annotation.Order;
import io.micronaut.mcp.server.exceptions.McpErrorExceptionMapper;
import io.micronaut.security.authentication.AuthorizationException;
import io.modelcontextprotocol.spec.McpError;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class McpToolExceptionMapper implements McpErrorExceptionMapper<Exception> {
    @Override
    public boolean canMap(Class<? extends Throwable> exceptionType) {
        return Exception.class.isAssignableFrom(exceptionType);
    }

    @Override
    public McpError map(Exception exception) {
        if (exception instanceof AuthorizationException) {
            return McpError.builder(-32001)
                .message("Forbidden: insufficient permissions")
                .build();
        }

        if (exception instanceof IllegalArgumentException && exception.getMessage() != null) {
            return McpError.builder(-32602)
                .message(exception.getMessage())
                .build();
        }

        log.error("MCP tool execution failed", exception);
        return McpError.builder(-32603)
            .message("MCP tool execution failed")
            .build();
    }
}
