package org.kafkahq.rest;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import org.kafkahq.service.NodeService;
import org.kafkahq.service.dto.node.ConfigDTO;
import org.kafkahq.service.dto.node.LogDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller("${kafkahq.server.base-path:}/api")
public class NodeResource {

    private final Logger log = LoggerFactory.getLogger(NodeResource.class);

    private NodeService nodeService;

    @Inject
    public NodeResource(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @Get("/cluster/nodes/configs")
    public List<ConfigDTO> fetchNodeConfigs(String clusterId, Integer nodeId) throws ExecutionException, InterruptedException {
        log.debug("Fetch node {} configs from cluster: {}", nodeId, clusterId);
        return nodeService.getConfigDTOList(clusterId, nodeId);
    }

    @Get("/cluster/nodes/logs")
    public List<LogDTO> fetchNodeLogs(String clusterId, Integer nodeId) throws ExecutionException, InterruptedException {
        log.debug("Fetch node {} logs from cluster: {}", nodeId, clusterId);
        return nodeService.getLogDTOList(clusterId, nodeId);
    }
}
