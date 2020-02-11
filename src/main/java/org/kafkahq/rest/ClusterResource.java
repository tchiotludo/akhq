package org.kafkahq.rest;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import org.kafkahq.models.Cluster;
import org.kafkahq.service.ClusterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

@Controller("${kafkahq.server.base-path:}/api")
public class ClusterResource {

    private final Logger log = LoggerFactory.getLogger(ClusterResource.class);

    private ClusterService clusterService;

    @Inject
    public ClusterResource(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    @Get("/clusters")
    public List<Cluster> fetchAllClusters() {
        log.debug("fetch all clusters");
        return clusterService.getAllClusters();
    }
}
