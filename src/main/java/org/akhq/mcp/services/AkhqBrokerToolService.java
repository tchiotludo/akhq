package org.akhq.mcp.services;

import jakarta.inject.Singleton;
import org.akhq.mcp.model.GetConfigsResult;
import org.akhq.mcp.model.ListBrokersResult;
import org.akhq.models.Cluster;
import org.akhq.repositories.ClusterRepository;
import org.akhq.repositories.ConfigRepository;

import java.util.Comparator;
import java.util.concurrent.ExecutionException;

@Singleton
public class AkhqBrokerToolService {
    private final ClusterRepository clusterRepository;
    private final ConfigRepository configRepository;

    public AkhqBrokerToolService(ClusterRepository clusterRepository, ConfigRepository configRepository) {
        this.clusterRepository = clusterRepository;
        this.configRepository = configRepository;
    }

    public ListBrokersResult listBrokers(String cluster) throws ExecutionException, InterruptedException {
        Cluster description = clusterRepository.get(cluster);
        Integer controllerId = description.getController() == null ? null : description.getController().getId();

        var brokers = description.getNodes().stream()
            .sorted(Comparator.comparingInt(org.akhq.models.Node::getId))
            .map(node -> new ListBrokersResult.Broker(
                node.getId(), node.getHost(), node.getPort(), node.getRack(),
                controllerId != null && controllerId == node.getId()
            ))
            .toList();

        return new ListBrokersResult(
            cluster, description.getId(), controllerId, brokers,
            "Found " + brokers.size() + " broker(s)."
        );
    }

    public GetConfigsResult getBrokerConfigs(String cluster, int brokerId, String search, Boolean onlyOverridden, Integer maxResults)
        throws ExecutionException, InterruptedException {
        boolean exists = clusterRepository.get(cluster).getNodes().stream().anyMatch(node -> node.getId() == brokerId);
        if (!exists) {
            throw new IllegalArgumentException("Broker '" + brokerId + "' doesn't exist");
        }

        return McpConfigs.result(
            cluster, "BROKER", String.valueOf(brokerId),
            configRepository.findByBroker(cluster, brokerId), search, onlyOverridden, maxResults
        );
    }
}
