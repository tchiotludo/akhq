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
import io.micronaut.views.ViewsRenderer;
import org.kafkahq.modules.RequestHelper;

import javax.inject.Inject;

@Controller("/errors")
public class NotFoundController extends AbstractController {
    private final ViewsRenderer viewsRenderer;
    private final RequestHelper requestHelper;

    @Value("${kafkahq.server.base-path}")
    protected String basePath;

    @Inject
    public NotFoundController(ViewsRenderer viewsRenderer, RequestHelper requestHelper) {
        this.viewsRenderer = viewsRenderer;
        this.requestHelper = requestHelper;
    }

    @Error(status = HttpStatus.NOT_FOUND, global = true)
    public HttpResponse notFound(HttpRequest request) {
        boolean isHtml = request.getHeaders()
            .accept()
            .stream()
            .anyMatch(mediaType -> mediaType.getName().contains(MediaType.TEXT_HTML));


        if (isHtml) {
            return HttpResponse
                .ok(viewsRenderer.render("notFound", templateData(requestHelper.getClusterId(request))))
                .contentType(MediaType.TEXT_HTML);
        }

        JsonError error = new JsonError("Page Not Found")
            .link(Link.SELF, Link.of(request.getUri()));

        return HttpResponse.<JsonError>notFound()
            .body(error);
    }
}