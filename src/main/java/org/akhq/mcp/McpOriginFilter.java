package org.akhq.mcp;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.filter.ServerFilterPhase;
import io.micronaut.http.hateoas.JsonError;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.Mcp;
import org.reactivestreams.Publisher;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rejects browser requests to the MCP endpoint coming from an origin that is not explicitly allowed.
 * <p>
 * The MCP specification requires servers to validate the {@code Origin} header to prevent DNS rebinding attacks,
 * where a malicious web page makes the browser of a user call an MCP server reachable from that user, such as a
 * local AKHQ. The MCP Java SDK ships such a validator, but the Micronaut MCP transport does not apply it.
 * <p>
 * Non browser MCP clients do not send an {@code Origin} header, so their requests are always accepted.
 */
@Slf4j
@Filter("/**")
@Requires(property = "akhq.mcp.enabled", value = "true")
public class McpOriginFilter implements HttpServerFilter {
    private final McpEndpoint mcpEndpoint;
    private final Set<String> allowedOrigins;

    public McpOriginFilter(McpEndpoint mcpEndpoint, Mcp mcp) {
        this.mcpEndpoint = mcpEndpoint;
        this.allowedOrigins = mcp.getAllowedOrigins().stream()
            .map(McpOriginFilter::normalize)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        if (isAllowed(request)) {
            return chain.proceed(request);
        }

        log.warn(
            "Rejected MCP request from origin '{}'. Add it to `akhq.mcp.allowed-origins` if it is a trusted MCP client.",
            request.getHeaders().getOrigin().orElse(null)
        );
        return Publishers.just(HttpResponse.status(HttpStatus.FORBIDDEN).body(new JsonError("Origin not allowed")));
    }

    boolean isAllowed(HttpRequest<?> request) {
        if (!mcpEndpoint.matches(request)) {
            return true;
        }

        Optional<String> origin = request.getHeaders().getOrigin();
        return origin.isEmpty() || allowedOrigins.contains(normalize(origin.get()));
    }

    @Override
    public int getOrder() {
        // Reject before any authentication work happens.
        return ServerFilterPhase.SECURITY.before();
    }

    private static String normalize(String origin) {
        return origin.trim().replaceAll("/+$", "").toLowerCase(Locale.ROOT);
    }
}
