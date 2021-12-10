package org.akhq.controllers;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micrometer.core.instrument.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.http.hateoas.Link;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.AuthorizationException;
import io.micronaut.security.rules.SecurityRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ApiException;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.ConcurrentConfigModificationException;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.InvalidRequestException;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.ResourceNotFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;

@Secured(SecurityRule.IS_ANONYMOUS)
@Slf4j
@Controller("/errors")
public class ErrorController extends AbstractController {
    // Kafka
    @Error(global = true)
    public HttpResponse<?> error(HttpRequest<?> request, ApiException e) {
        return renderExecption(request, e);
    }

    // Registry
    @Error(global = true)
    public HttpResponse<?> error(HttpRequest<?> request, RestClientException e) {
        return renderExecption(request, e);
    }

    // Connect
    @Error(global = true)
    public HttpResponse<?> error(HttpRequest<?> request, InvalidRequestException e) {
        return renderExecption(request, e);
    }

    @Error(global = true)
    public HttpResponse<?> error(HttpRequest<?> request, ResourceNotFoundException e) {
        return renderExecption(request, e);
    }

    @Error(global = true)
    public HttpResponse<?> error(HttpRequest<?> request, ConcurrentConfigModificationException e) {
        return renderExecption(request, e);
    }

    // Akhq

    @Error(global = true)
    public HttpResponse<?> error(HttpRequest<?> request, IllegalArgumentException e) {
        return renderExecption(request, e);
    }

    private HttpResponse<?> renderExecption(HttpRequest<?> request, Exception e) {
        JsonError error = new JsonError(e.getMessage())
            .link(Link.SELF, Link.of(request.getUri()));

        return HttpResponse.<JsonError>status(HttpStatus.CONFLICT)
            .body(error);
    }

    @Error(global = true)
    public HttpResponse<?> error(HttpRequest<?> request, AuthorizationException e) throws URISyntaxException {
        if (request.getUri().toString().startsWith("/api")) {
            return HttpResponse.unauthorized().body(new JsonError("Unauthorized"));
        }

        return HttpResponse.temporaryRedirect(this.uri("/ui/login"));
    }

    @Error(global = true)
    public HttpResponse<?> error(HttpRequest<?> request, Throwable e) {
        log.error(e.getMessage(), e);

        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));

        JsonError error = new JsonError("Internal Server Error: " + e.getMessage())
            .link(Link.SELF, Link.of(request.getUri()))
            .embedded(
                "stacktrace",
                new JsonError(stringWriter.toString())
            );

        return HttpResponse.<JsonError>serverError()
            .body(error);
    }

    @Error(status = HttpStatus.NOT_FOUND, global = true)
    public HttpResponse<?> notFound(HttpRequest<?> request) throws URISyntaxException {
        if (request.getPath().equals("/") && StringUtils.isNotEmpty(getBasePath())) {
            return HttpResponse.temporaryRedirect(this.uri("/"));
        }

        JsonError error = new JsonError("Page Not Found")
            .link(Link.SELF, Link.of(request.getUri()));

        return HttpResponse.<JsonError>notFound()
            .body(error);
    }
}
