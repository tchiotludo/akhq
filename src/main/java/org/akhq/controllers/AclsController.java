package org.akhq.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.views.View;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import org.akhq.configs.Role;
import org.akhq.models.AccessControl;
import org.akhq.repositories.AccessControlListRepository;
import org.apache.kafka.common.resource.ResourceType;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

@Secured(Role.ROLE_ACLS_READ)
@Controller("${akhq.server.base-path:}/")
public class AclsController extends AbstractController {
    private final AccessControlListRepository aclRepository;

    @Inject
    public AclsController(AccessControlListRepository aclRepository) {
        this.aclRepository = aclRepository;
    }

    @Get("{cluster}/acls")
    @View("aclsList")
    @Hidden
    public HttpResponse<?> list(
        HttpRequest<?> request,
        String cluster,
        Optional<String> search
    ) throws ExecutionException, InterruptedException {
        return this.template(
            request,
            cluster,
            "search", search,
            "acls", aclRepository.findAll(cluster, search)
        );
    }

    @Operation(tags = {"acls"}, summary = "List all acls")
    @Get("api/{cluster}/acls")
    public List<AccessControl> listApi(HttpRequest<?> request, String cluster, Optional<String> search) throws ExecutionException, InterruptedException {
        return aclRepository.findAll(cluster, search);
    }

    @View("acl")
    @Get("{cluster}/acls/{principal}")
    @Hidden
    public HttpResponse<?> principal(HttpRequest<?> request, String cluster, String principal) throws ExecutionException, InterruptedException {
        return this.template(
            request,
            cluster,
            "tab", "topic",
            "acl", aclRepository.findByPrincipal(cluster, principal, Optional.of(ResourceType.TOPIC))
        );
    }

    @Operation(tags = {"acls"}, summary = "Get acls for a principal")
    @Get("api/{cluster}/acls/{principal}")
    public AccessControl principalApi(
        String cluster,
        String principal,
        Optional<ResourceType> resourceType
    ) throws ExecutionException, InterruptedException {
        return aclRepository.findByPrincipal(cluster, principal, resourceType);
    }

    @View("acl")
    @Get("{cluster}/acls/{principal}/group")
    @Hidden
    public HttpResponse<?> tab(HttpRequest<?> request, String cluster, String principal) throws ExecutionException, InterruptedException {
        return this.template(
            request,
            cluster,
            "tab", "group",
            "acl", aclRepository.findByPrincipal(cluster, principal, Optional.of(ResourceType.GROUP))
        );
    }
}
