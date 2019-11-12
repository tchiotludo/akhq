package org.kafkahq.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.views.View;
import org.kafkahq.configs.Role;
import org.kafkahq.repositories.AccessControlListRepository;

import javax.inject.Inject;
import java.util.Optional;

@Secured(Role.ROLE_ACLS_READ)
@Controller("${kafkahq.server.base-path:}/{cluster}/acls")
public class AclsController extends AbstractController {

    private AccessControlListRepository aclRepository;

    @Inject
    public AclsController(AccessControlListRepository aclRepository) {
        this.aclRepository = aclRepository;
    }

    @Get
    @View("aclsList")
    public HttpResponse list(HttpRequest request, String cluster,
                             Optional<String> search) {
        return this.template(
                request,
                cluster,
                "search", search,
                "acls", aclRepository.findAll(cluster, search)
        );
    }

    @View("acl")
    @Get("{principal}")
    public HttpResponse principal (HttpRequest request, String cluster, String principal){
        return this.template(
                request,
                cluster,
                "tab", "topic",
                "acl", aclRepository.findByPrincipal(cluster, principal, Optional.of("topic"))
        );
    }

    @View("acl")
    @Get("{principal}/{tab:(group)}")
    public HttpResponse tab (HttpRequest request, String cluster, String principal, String tab){
        return this.template(
                request,
                cluster,
                "tab", tab,
                "acl", aclRepository.findByPrincipal(cluster, principal, Optional.of(tab))
        );
    }

}
