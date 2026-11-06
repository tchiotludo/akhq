package org.akhq.mcp;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.Hidden;

/**
 * Answers the Streamable HTTP methods the stateless MCP transport does not support.
 * <p>
 * The MCP specification expects {@code 405 Method Not Allowed} for a {@code GET} when the server offers no SSE
 * stream, and for a {@code DELETE} when it has no session to terminate. Without this controller, the missing route
 * reaches AKHQ's generic error handler and is answered with a {@code 500}.
 */
@Hidden
@Controller("${micronaut.mcp.server.endpoint:/mcp}")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Requires(property = "akhq.mcp.enabled", value = "true")
public class McpMethodNotAllowedController {
    @Get
    public HttpResponse<?> get() {
        return methodNotAllowed();
    }

    @Delete
    public HttpResponse<?> delete() {
        return methodNotAllowed();
    }

    private static HttpResponse<?> methodNotAllowed() {
        return HttpResponse.status(HttpStatus.METHOD_NOT_ALLOWED)
            .header(HttpHeaders.ALLOW, HttpMethod.POST.name());
    }
}
