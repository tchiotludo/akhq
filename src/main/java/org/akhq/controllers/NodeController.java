package org.akhq.controllers;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.swagger.v3.oas.annotations.Operation;
import org.akhq.configs.Role;
import org.akhq.models.Cluster;
import org.akhq.models.Config;
import org.akhq.models.LogDir;
import org.akhq.models.Node;
import org.akhq.repositories.ClusterRepository;
import org.akhq.repositories.ConfigRepository;
import org.akhq.repositories.LogDirRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

@Secured(Role.ROLE_NODE_READ)
@Controller("${akhq.server.base-path:}/")
public class NodeController extends AbstractController {
    private final ClusterRepository clusterRepository;
    private final ConfigRepository configRepository;
    private final LogDirRepository logDirRepository;

    @Inject
    public NodeController(ClusterRepository clusterRepository, ConfigRepository configRepository, LogDirRepository logDirRepository) {
        this.clusterRepository = clusterRepository;
        this.configRepository = configRepository;
        this.logDirRepository = logDirRepository;
    }

    @Get("api/{cluster}/node")
    @Operation(tags = {"node"}, summary = "List all nodes")
    public Cluster list(String cluster) throws ExecutionException, InterruptedException {
        return this.clusterRepository.get(cluster);
    }

    @Get("api/{cluster}/node/{nodeId}")
    @Operation(tags = {"node"}, summary = "Retrieve a nodes")
    public Node node(String cluster, Integer nodeId) throws ExecutionException, InterruptedException {
        return findNode(cluster, nodeId);
    }

    @Get("api/{cluster}/node/{nodeId}/logs")
    @Operation(tags = {"node"}, summary = "List all logs for a node")
    public List<LogDir> nodeLog(String cluster, Integer nodeId) throws ExecutionException, InterruptedException {
        return logDirRepository.findByBroker(cluster, nodeId);
    }

    @Get("api/{cluster}/node/{nodeId}/configs")
    @Operation(tags = {"node"}, summary = "List all configs for a node")
    public List<Config> nodeConfig(String cluster, Integer nodeId) throws ExecutionException, InterruptedException {
        List<Config> configs = this.configRepository.findByBroker(cluster, nodeId);

        if (configs == null) {
            return new ArrayList<>();
        }

        configs.sort((o1, o2) -> o1.isReadOnly() == o2.isReadOnly() ? 0 :
            (o1.isReadOnly() ? 1 : -1 )
        );

        return configs;
    }

    @Post("api/{cluster}/node/{nodeId}/configs")
    @Operation(tags = {"node"}, summary = "Update configs for a node")
    public List<Config> nodeConfigUpdate(String cluster, Integer nodeId, Map<String, String> configs) throws ExecutionException, InterruptedException {
        List<Config> updated = ConfigRepository.updatedConfigs(configs, this.configRepository.findByBroker(cluster, nodeId), false);

        if (updated.size() == 0) {
            throw new IllegalArgumentException("No config to update");
        }

        this.configRepository.updateBroker(
            cluster,
            nodeId,
            updated
        );

        return updated;
    }

    private Node findNode(String cluster, Integer nodeId) throws ExecutionException, InterruptedException {
        return this.clusterRepository.get(cluster)
                .getNodes()
                .stream()
                .filter(e -> e.getId() == nodeId)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Node '" + nodeId + "' doesn't exist"));
    }
}
