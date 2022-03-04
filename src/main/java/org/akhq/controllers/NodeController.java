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

import org.akhq.repositories.TopicRepository;
import org.akhq.repositories.TopicRepository.TopicListView;
import org.akhq.models.Partition;
import java.util.Optional;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import jakarta.inject.Inject;

@Secured(Role.ROLE_NODE_READ)
@Controller
public class NodeController extends AbstractController {
    private final ClusterRepository clusterRepository;
    private final ConfigRepository configRepository;
    private final LogDirRepository logDirRepository;
    @Inject
    private TopicRepository topicRepository;

    @Inject
    public NodeController(ClusterRepository clusterRepository, ConfigRepository configRepository, LogDirRepository logDirRepository) {
        this.clusterRepository = clusterRepository;
        this.configRepository = configRepository;
        this.logDirRepository = logDirRepository;
    }

    @Get("api/{cluster}/node/partitions")
    @Operation(tags = {"topic"}, summary = "partition counts")
    public Map<String, Map<Long,Long>> nodePartitions( String cluster ) throws ExecutionException, InterruptedException {
        List<Partition> thePartitions = new ArrayList<>();
        Integer totalPartitions = 0;
        Integer nodeId = 0;
        List<String> topicNames = this.topicRepository.all(cluster, TopicRepository.TopicListView.HIDE_INTERNAL, Optional.empty());
        final Map<Long, Long> nodePartitionCounts = new HashMap<>();
        // Get total count of paritions across all topics
        for (String topicName : topicNames) {
            thePartitions = this.topicRepository.findByName(cluster, topicName).getPartitions();
            totalPartitions += thePartitions.size();   
        }
        // Get partiton counts for each node
        for ( Node node : this.clusterRepository.get(cluster).getNodes()){
            for (String topicName : topicNames) {
                thePartitions = this.topicRepository.findByName(cluster, topicName).getPartitions();
                for (Partition part : thePartitions){
                    nodePartitionCounts.put((long)node.getId(), nodePartitionCounts.getOrDefault((long)node.getId(),0L) + part.getNodes().stream().filter(e -> e.getId() == node.getId()).count());
                }
                
            }
        }
        final Map<String, Map<Long,Long>> results = new HashMap<>();
        final Map<Long, Long> totalParts = new HashMap<>();
        totalParts.put(0L,(long)totalPartitions);
        results.put("total",totalParts);
        results.put("nodes",nodePartitionCounts);
        return results;
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
