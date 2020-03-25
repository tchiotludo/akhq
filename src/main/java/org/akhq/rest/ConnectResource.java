package org.akhq.rest;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import org.akhq.service.ConnectService;
import org.akhq.service.dto.ConnectDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

@Controller("${akhq.server.base-path:}/api")
public class ConnectResource {

    private final Logger log = LoggerFactory.getLogger(ConnectResource.class);

    private ConnectService connectService;

    @Inject
    public ConnectResource(ConnectService connectService) {
        this.connectService = connectService;
    }

    @Get("/connects")
    public List<ConnectDTO> fetchAllConnectsFromCluster(String clusterId) {
        log.debug("Fetch all connects from cluster {}", clusterId);
        return connectService.getAllConnectsFromCLuster(clusterId);
    }
}
