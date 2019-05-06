package org.kafkahq.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.views.View;
import org.kafkahq.configs.Role;
import org.kafkahq.repositories.KsqlRepository;

import javax.inject.Inject;

@Secured(Role.ROLE_CONNECT_READ)
@Controller("${kafkahq.server.base-path:}/{cluster}/ksql")
public class KsqlController extends AbstractController {
    private KsqlRepository ksqlRepository;

    @Inject
    public KsqlController(KsqlRepository ksqlRepository) {
        this.ksqlRepository = ksqlRepository;
    }

    @View("connectList")
    @Get
    public HttpResponse list(HttpRequest request, String cluster) {

        ksqlRepository.showProperties(cluster);

        return this.template(
            request,
            cluster
        );
    }
}
