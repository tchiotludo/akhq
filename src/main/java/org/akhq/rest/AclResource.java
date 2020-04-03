package org.akhq.rest;


import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import lombok.extern.slf4j.Slf4j;
import org.akhq.models.AccessControlList;
import org.akhq.service.AclService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Slf4j
@Controller("${akhq.server.base-path:}/api")
public class AclResource {

    private AclService aclService;

    public AclResource(AclService aclService) {
        this.aclService = aclService;
    }


    @Get("/aclsList")
    public List<String> getAclsList(String clusterId, Optional<String> search) {
        return aclService.findAll(clusterId, search);
    }

}
