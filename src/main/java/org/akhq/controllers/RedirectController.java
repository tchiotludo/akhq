package org.akhq.controllers;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.Hidden;

import java.net.URISyntaxException;

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller
@Hidden
public class RedirectController extends AbstractController {
    @Get
    public HttpResponse<?> slash() throws URISyntaxException {
        return HttpResponse.temporaryRedirect(this.uri("/ui"));
    }
}
