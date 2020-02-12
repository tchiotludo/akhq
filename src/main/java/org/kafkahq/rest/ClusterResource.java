package org.kafkahq.rest;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import org.kafkahq.models.Cluster;
import org.kafkahq.models.Node;
import org.kafkahq.service.ClusterService;
import org.kafkahq.service.dto.ClusterDTO;
import org.kafkahq.service.mapper.ClusterMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
        log.debug("fetch all clusters");
        return clusterService
                .getAllClusters();
    }

    @Get("/cluster/nodes")
    public List<Node> fetchAllNodesFromCluster(String clusterId) throws ExecutionException, InterruptedException {
        log.debug("fetch all nodes from cluster {}", clusterId);
        return clusterService.getAllNodesFromCluster(clusterId);
    }
}
