package org.kafkahq.service;

import org.kafkahq.models.Cluster;
import org.kafkahq.models.Node;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.repositories.ClusterRepository;
import org.kafkahq.service.dto.ClusterDTO;
import org.kafkahq.service.mapper.ClusterMapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class ClusterService {

    private KafkaModule kafkaModule;

    private ClusterRepository clusterRepository;

    @Inject
    public ClusterService(KafkaModule kafkaModule, ClusterRepository clusterRepository) {
        this.kafkaModule = kafkaModule;
        this.clusterRepository = clusterRepository;
    }

    public List<ClusterDTO> getAllClusters() {
        return kafkaModule
                .getClustersList()
                .stream()
                .map(ClusterDTO::new)
                .collect(Collectors.toList());
    }

    public List<Node> getAllNodesFromCluster(String clusterId) throws ExecutionException, InterruptedException {
        return clusterRepository.get(clusterId).getNodes();

    }
}
