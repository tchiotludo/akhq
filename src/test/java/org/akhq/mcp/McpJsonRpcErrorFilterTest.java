package org.akhq.mcp;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpJsonRpcErrorFilterTest {
    @Test
    void answersRequestLevelErrorsWithOk() {
        assertEquals(HttpStatus.OK, rewrite(HttpStatus.BAD_REQUEST, McpSchema.ErrorCodes.METHOD_NOT_FOUND).getStatus());
        assertEquals(HttpStatus.OK, rewrite(HttpStatus.BAD_REQUEST, McpSchema.ErrorCodes.INVALID_PARAMS).getStatus());
        assertEquals(HttpStatus.OK, rewrite(HttpStatus.INTERNAL_SERVER_ERROR, McpSchema.ErrorCodes.INTERNAL_ERROR).getStatus());
        assertEquals(HttpStatus.OK, rewrite(HttpStatus.INTERNAL_SERVER_ERROR, -32001).getStatus());
    }

    @Test
    void keepsTransportLevelErrors() {
        assertEquals(HttpStatus.BAD_REQUEST, rewrite(HttpStatus.BAD_REQUEST, McpSchema.ErrorCodes.PARSE_ERROR).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, rewrite(HttpStatus.BAD_REQUEST, McpSchema.ErrorCodes.INVALID_REQUEST).getStatus());
    }

    @Test
    void replacesSerializedExceptionsWithAJsonRpcError() {
        McpError error = McpError.builder(McpSchema.ErrorCodes.INVALID_REQUEST).message("invalid").build();

        MutableHttpResponse<?> response = McpJsonRpcErrorFilter.rewrite(HttpResponse.badRequest(error));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        Map<?, ?> body = assertInstanceOf(Map.class, response.body());
        assertEquals("2.0", body.get("jsonrpc"));
        assertTrue(body.containsKey("id"));
        assertNull(body.get("id"));
        assertEquals(Map.of("code", McpSchema.ErrorCodes.INVALID_REQUEST, "message", "invalid"), body.get("error"));
    }

    @Test
    void leavesOtherResponsesUntouched() {
        MutableHttpResponse<?> result = HttpResponse.ok(McpSchema.JSONRPCResponse.result(1, Map.of()));
        assertSame(result, McpJsonRpcErrorFilter.rewrite(result));

        MutableHttpResponse<?> unauthorized = HttpResponse.unauthorized();
        assertEquals(HttpStatus.UNAUTHORIZED, McpJsonRpcErrorFilter.rewrite(unauthorized).getStatus());
    }

    private static MutableHttpResponse<?> rewrite(HttpStatus status, int code) {
        McpSchema.JSONRPCResponse body = McpSchema.JSONRPCResponse.error(
            1,
            new McpSchema.JSONRPCResponse.JSONRPCError(code, "error")
        );
        return McpJsonRpcErrorFilter.rewrite(HttpResponse.status(status).body(body));
    }
}
