package org.akhq.controllers;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.Hidden;
import org.akhq.modules.KafkaModule;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller
@Hidden
public class RedirectController extends AbstractController {
    @Inject
    private KafkaModule kafkaModule;

    @Value("${akhq.server.base-path}")
    protected String basePath;

    @Get
    public HttpResponse<?> slash() throws URISyntaxException {
        return HttpResponse.temporaryRedirect(this.uri("/ui"));
    }

    @Get("${akhq.server.base-path:}")
    public HttpResponse<?> home() throws URISyntaxException {
        return this.slash();
    }

    @Get("${akhq.server.base-path:}/{cluster:(?!login|ui)[^/]+}")
    public HttpResponse<?> topic(String cluster) throws URISyntaxException {
        return HttpResponse.redirect(this.uri("/" + cluster + "/topic"));
    }
}
