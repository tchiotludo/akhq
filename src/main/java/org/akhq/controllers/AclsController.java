package org.akhq.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.swagger.v3.oas.annotations.Operation;
import org.akhq.configs.Role;
import org.akhq.models.AccessControl;
import org.akhq.modules.KafkaModule;
import org.akhq.repositories.AccessControlListRepository;
import org.apache.kafka.common.resource.ResourceType;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import jakarta.inject.Inject;

@Secured(Role.ROLE_ACLS_READ)
@Controller("/api/{cluster}/acls")
public class AclsController extends AbstractController {
    private final AccessControlListRepository aclRepository;

    @Inject
    public AclsController(AccessControlListRepository aclRepository) {
        this.aclRepository = aclRepository;
    }
    @Inject
    private KafkaModule kafkaModule;

    @Operation(tags = {"acls"}, summary = "List all acls")
    @Get
    public List<AccessControl> list(HttpRequest<?> request, String cluster, Optional<String> search) throws ExecutionException, InterruptedException {
        if(kafkaModule.clusterExists(cluster)) {
            return aclRepository.findAll(cluster, search);
        } else {
            HttpResponse.status(HttpStatus.NOT_FOUND);
            return null;
        }
    }

    @Operation(tags = {"acls"}, summary = "Get acls for a principal")
    @Get("{principal}")
    public AccessControl principal(
        String cluster,
        String principal,
        Optional<ResourceType> resourceType
    ) throws ExecutionException, InterruptedException {
        if(kafkaModule.clusterExists(cluster)) {
            return aclRepository.findByPrincipal(cluster, principal, resourceType);
        } else {
            HttpResponse.status(HttpStatus.NOT_FOUND);
            return null;
        }
    }
}
