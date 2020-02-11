package org.kafkahq.rest;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import org.kafkahq.configs.Connect;
import org.kafkahq.service.ConnectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

@Controller("${kafkahq.server.base-path:}/api")
public class ConnectResource {

    private final Logger log = LoggerFactory.getLogger(ConnectResource.class);

    private ConnectService connectService;

    @Inject
    public ConnectResource(ConnectService connectService) {
        this.connectService = connectService;
    }
    
    @Get("/connects")
    public List<Connect> fetchAllConnectsFromCluster(String clusterId) {
        log.debug("fetch all connects from cluster {}", clusterId);
        return connectService.getAllConnectsFromCLuster(clusterId);
    }
}
