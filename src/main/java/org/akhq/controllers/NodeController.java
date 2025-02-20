package org.akhq.controllers;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.inject.Inject;
import lombok.Builder;
import lombok.Getter;
import org.akhq.configs.security.Role;
import org.akhq.models.*;
import org.akhq.repositories.ClusterRepository;
import org.akhq.repositories.ConfigRepository;
import org.akhq.repositories.LogDirRepository;
import org.akhq.repositories.TopicRepository;
import org.akhq.security.annotation.AKHQSecured;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@AKHQSecured(resource = Role.Resource.NODE, action = Role.Action.READ)
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

    @Introspected
    @Builder(toBuilder = true)
    @Getter
    public static class NodePartition {
        int id;
        int countLeader;
        int countInSyncReplicas;
        long totalPartitions;
    }

    @Get("api/{cluster}/node/partitions")
    @Operation(tags = {"topic"}, summary = "Partition counts")
    public List<NodePartition> nodePartitions(String cluster) throws ExecutionException, InterruptedException {
        checkIfClusterAllowed(cluster);

        List<String> topicNames = this.topicRepository.all(cluster, TopicRepository.TopicListView.HIDE_INTERNAL,
            Optional.empty(), buildUserBasedResourceFilters(cluster));
        List<Topic> topics = this.topicRepository.findByName(cluster, topicNames);
        long totalPartitions = topics
            .stream()
            .mapToInt(t -> t.getPartitions().size())
            .sum();
        return topics
            .stream()
            .flatMap(topic -> topic.getPartitions().stream())
            .flatMap(partition -> partition.getNodes()
                .stream()
                .map(n -> NodePartition.builder()
                    .id(n.getId())
                    .countLeader(n.isLeader() ? 1 : 0)
                    .countInSyncReplicas(n.isInSyncReplicas() ? 1 : 0)
                    .build()
                )
            )
            .collect(Collectors.groupingBy(NodePartition::getId))
            .entrySet()
            .stream()
            .map(n -> NodePartition.builder()
                .id(n.getKey())
                .countLeader(n.getValue().stream().mapToInt(NodePartition::getCountLeader).sum())
                .countInSyncReplicas(n.getValue().stream().mapToInt(NodePartition::getCountInSyncReplicas).sum())
                .totalPartitions(totalPartitions)
                .build()
            )
            .collect(Collectors.toList());
    }

    @Get("api/{cluster}/node")
    @Operation(tags = {"node"}, summary = "List all nodes")
    public Cluster list(String cluster) throws ExecutionException, InterruptedException {
        checkIfClusterAllowed(cluster);

        return this.clusterRepository.get(cluster);
    }

    @Get("api/{cluster}/node/{nodeId}")
    @Operation(tags = {"node"}, summary = "Retrieve a node")
    public Node node(String cluster, Integer nodeId) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, nodeId.toString());

        return findNode(cluster, nodeId);
    }

    @Get("api/{cluster}/node/{nodeId}/logs")
    @Operation(tags = {"node"}, summary = "List all logs for a node")
    public List<LogDir> nodeLog(String cluster, Integer nodeId) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, nodeId.toString());

        return logDirRepository.findByBroker(cluster, nodeId);
    }

    @Get("api/{cluster}/node/{nodeId}/configs")
    @AKHQSecured(resource = Role.Resource.NODE, action = Role.Action.READ_CONFIG)
    @Operation(tags = {"node"}, summary = "List all configs for a node")
    public List<Config> nodeConfig(String cluster, Integer nodeId) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, nodeId.toString());

        List<Config> configs = this.configRepository.findByBroker(cluster, nodeId);

        if (configs == null) {
            return new ArrayList<>();
        }

        configs.sort((o1, o2) -> o1.isReadOnly() == o2.isReadOnly() ? 0 :
            (o1.isReadOnly() ? 1 : -1)
        );

        return configs;
    }

    @AKHQSecured(resource = Role.Resource.NODE, action = Role.Action.ALTER_CONFIG)
    @Post("api/{cluster}/node/{nodeId}/configs")
    @Operation(tags = {"node"}, summary = "Update configs for a node")
    public List<Config> nodeConfigUpdate(String cluster, Integer nodeId, @Body("configs") Map<String, String> configs) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, nodeId.toString());

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
        checkIfClusterAndResourceAllowed(cluster, nodeId.toString());

        return this.clusterRepository.get(cluster)
            .getNodes()
            .stream()
            .filter(e -> e.getId() == nodeId)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Node '" + nodeId + "' doesn't exist"));
    }
}
