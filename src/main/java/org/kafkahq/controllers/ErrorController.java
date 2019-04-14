package org.kafkahq.controllers;


import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.http.hateoas.Link;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.ViewsRenderer;
import lombok.extern.slf4j.Slf4j;
import org.kafkahq.modules.RequestHelper;

import javax.inject.Inject;
import java.io.PrintWriter;
import java.io.StringWriter;

@Secured(SecurityRule.IS_ANONYMOUS)
@Slf4j
@Controller("/errors")
public class ErrorController extends AbstractController {
    private final ViewsRenderer viewsRenderer;
    private final RequestHelper requestHelper;

    @Value("${kafkahq.server.base-path}")
    protected String basePath;

    @Inject
    public ErrorController(ViewsRenderer viewsRenderer, RequestHelper requestHelper) {
        this.viewsRenderer = viewsRenderer;
        this.requestHelper = requestHelper;
    }

    @Error(global = true)
    public HttpResponse error(HttpRequest request, Throwable e) {
        log.error(e.getMessage(), e);

        if (isHtml(request)) {
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));

            return HttpResponse
                .ok(viewsRenderer.render("errors/internal", templateData(
                    requestHelper.getClusterId(request),
                    "message", e.getMessage(),
                    "stacktrace", stringWriter.toString()
                )))
                .contentType(MediaType.TEXT_HTML);
        }

        JsonError error = new JsonError("Internal Server Error: " + e.getMessage())
            .link(Link.SELF, Link.of(request.getUri()));

        return HttpResponse.<JsonError>serverError()
            .body(error);
    }

    @Error(status = HttpStatus.NOT_FOUND, global = true)
    public HttpResponse notFound(HttpRequest request) {
        if (isHtml(request)) {
            return HttpResponse
                .ok(viewsRenderer.render("errors/notFound", templateData(requestHelper.getClusterId(request))))
                .contentType(MediaType.TEXT_HTML);
        }

        JsonError error = new JsonError("Page Not Found")
            .link(Link.SELF, Link.of(request.getUri()));

        return HttpResponse.<JsonError>notFound()
            .body(error);
    }

    private boolean isHtml(HttpRequest request) {
        return request.getHeaders()
                .accept()
                .stream()
                .anyMatch(mediaType -> mediaType.getName().contains(MediaType.TEXT_HTML));
    }
}