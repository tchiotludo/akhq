package org.akhq.mcp;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.annotation.Order;
import io.micronaut.mcp.server.exceptions.McpErrorExceptionMapper;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Maps the unexpected failures of a tool to an internal JSON-RPC error without leaking their details. Invalid
 * arguments and missing permissions are not handled here: tools report them as tool execution errors.
 */
@Singleton
@Requires(property = "akhq.mcp.enabled", value = "true")
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class McpToolExceptionMapper implements McpErrorExceptionMapper<Exception> {
    @Override
    public boolean canMap(Class<? extends Throwable> exceptionType) {
        return Exception.class.isAssignableFrom(exceptionType);
    }

    @Override
    public McpError map(Exception exception) {
        log.error("MCP tool execution failed", exception);
        return McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
            .message("MCP tool execution failed")
            .build();
    }
}
