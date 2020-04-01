package org.akhq.rest;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import lombok.extern.slf4j.Slf4j;
import org.akhq.service.NodeService;
import org.akhq.service.dto.node.ConfigDTO;
import org.akhq.service.dto.node.ConfigOperationDTO;
import org.akhq.service.dto.node.LogDTO;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Controller("${akhq.server.base-path:}/api")
public class NodeResource {

    private NodeService nodeService;

    @Inject
    public NodeResource(NodeService nodeService) { this.nodeService = nodeService; }

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

    @Post("cluster/nodes/update-configs")
    public List<ConfigDTO> updateNodeConfigs(@Body ConfigOperationDTO configOperation) throws Throwable {
        log.debug("update node {} configs from cluster: {}", configOperation.getNodeId(), configOperation.getClusterId());
        return nodeService.updateConfigs(configOperation);
    }
}
