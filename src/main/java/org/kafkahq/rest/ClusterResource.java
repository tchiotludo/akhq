package org.kafkahq.rest;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import org.kafkahq.service.ClusterService;
import org.kafkahq.service.dto.ClusterDTO;
import org.kafkahq.service.dto.NodeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller("${kafkahq.server.base-path:}/api")
public class ClusterResource {

    private final Logger log = LoggerFactory.getLogger(ClusterResource.class);

    private ClusterService clusterService;

    @Inject
    public ClusterResource(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    @Get("/clusters")
    public List<ClusterDTO> fetchAllClusters() {
        log.debug("Fetch all clusters");
        return clusterService
                .getAllClusters();
    }

    @Get("/cluster/nodes")
    public List<NodeDTO> fetchAllNodesFromCluster(String clusterId) throws ExecutionException, InterruptedException {
        log.debug("Fetch all nodes from cluster {}", clusterId);
        return clusterService.getAllNodesFromCluster(clusterId);
    }
}
