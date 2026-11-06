package org.akhq.mcp;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.reactivestreams.Publisher;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Aligns the HTTP status of MCP JSON-RPC errors with the Streamable HTTP transport.
 * <p>
 * The Micronaut MCP transport maps every JSON-RPC error to an HTTP error status: {@code 400} for an unknown method
 * or invalid params, {@code 500} for a failed or forbidden tool call. MCP clients treat an HTTP error as a transport
 * failure and drop the connection, while the request was processed and its JSON-RPC error is the answer. This filter
 * therefore answers such errors with {@code 200}, and only keeps an HTTP error when the message itself could not be
 * accepted: a parse error or an invalid JSON-RPC message.
 * <p>
 * For an invalid message, the transport serializes the raw {@link McpError} exception, stack trace included. It is
 * replaced by a JSON-RPC error without an id, as the specification suggests.
 */
@Filter("/**")
@Requires(property = "akhq.mcp.enabled", value = "true")
public class McpJsonRpcErrorFilter implements HttpServerFilter {
    private static final Set<Integer> TRANSPORT_ERRORS = Set.of(
        McpSchema.ErrorCodes.PARSE_ERROR,
        McpSchema.ErrorCodes.INVALID_REQUEST
    );

    private final McpEndpoint mcpEndpoint;

    public McpJsonRpcErrorFilter(McpEndpoint mcpEndpoint) {
        this.mcpEndpoint = mcpEndpoint;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        if (request.getMethod() != HttpMethod.POST || !mcpEndpoint.matches(request)) {
            return chain.proceed(request);
        }

        return Publishers.map(chain.proceed(request), McpJsonRpcErrorFilter::rewrite);
    }

    @SuppressWarnings("unchecked")
    static MutableHttpResponse<?> rewrite(MutableHttpResponse<?> response) {
        Object body = response.getBody().orElse(null);

        if (body instanceof McpError error) {
            return ((MutableHttpResponse<Object>) response).body(errorWithoutId(jsonRpcError(error)));
        }

        if (body instanceof McpSchema.JSONRPCResponse jsonRpcResponse
            && jsonRpcResponse.error() != null
            && response.getStatus().getCode() >= 400
            && !TRANSPORT_ERRORS.contains(jsonRpcResponse.error().code())) {
            return response.status(HttpStatus.OK);
        }

        return response;
    }

    /**
     * The SDK {@link McpSchema.JSONRPCResponse} refuses a {@code null} id, which JSON-RPC mandates when the id of the
     * invalid message cannot be determined.
     */
    private static Map<String, Object> errorWithoutId(McpSchema.JSONRPCResponse.JSONRPCError error) {
        Map<String, Object> jsonRpcError = new LinkedHashMap<>();
        jsonRpcError.put("code", error.code());
        jsonRpcError.put("message", error.message());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jsonrpc", McpSchema.JSONRPC_VERSION);
        body.put("id", null);
        body.put("error", jsonRpcError);
        return body;
    }

    private static McpSchema.JSONRPCResponse.JSONRPCError jsonRpcError(McpError error) {
        if (error.getJsonRpcError() != null) {
            return error.getJsonRpcError();
        }

        return new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INVALID_REQUEST, error.getMessage());
    }
}
