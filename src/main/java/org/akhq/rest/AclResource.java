//package org.akhq.rest;
//
//
//import io.micronaut.http.annotation.Controller;
//import io.micronaut.http.annotation.Get;
//import lombok.extern.slf4j.Slf4j;
//import org.akhq.service.AclService;
//import org.akhq.service.dto.acls.AclsDTO;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.concurrent.ExecutionException;
//
//@Slf4j
//@Controller("${akhq.server.base-path:}/api")
//public class AclResource {
//
//    private AclService aclService;
//
//    public AclResource(AclService aclService) {
//        this.aclService = aclService;
//    }
//
//    @Get("/aclsList")
//    public List<AclsDTO> getAclsList(String clusterId, Optional<String> search) {
//        log.debug("Fetching ACLs for cluster {}", clusterId);
//        return aclService.findAll(clusterId, search);
//    }
//
//    @Get("/aclsByPrincipal")
//    public List<AclsDTO> getAclsByPrincipal(String clusterId, String principalEncoded, String resourceType) {
//        log.debug("Fetching ACLs by principal for cluster: {} {}", clusterId, resourceType);
//        return aclService.findByPrincipal(clusterId, principalEncoded, resourceType);
//    }
//}
