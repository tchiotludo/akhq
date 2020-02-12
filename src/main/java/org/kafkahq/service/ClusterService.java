package org.kafkahq.service;

import org.kafkahq.modules.KafkaModule;
import org.kafkahq.repositories.ClusterRepository;
import org.kafkahq.service.dto.ClusterDTO;
import org.kafkahq.service.dto.NodeDTO;
import org.kafkahq.service.mapper.NodeMapper;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class ClusterService {

    private KafkaModule kafkaModule;

    private ClusterRepository clusterRepository;

    private NodeMapper nodeMapper;

    @Inject
    public ClusterService(KafkaModule kafkaModule, ClusterRepository clusterRepository, NodeMapper nodeMapper) {
        this.kafkaModule = kafkaModule;
        this.clusterRepository = clusterRepository;
        this.nodeMapper = nodeMapper;
    }

    public List<ClusterDTO> getAllClusters() {
        return kafkaModule
                .getClustersList()
                .stream()
                .map(ClusterDTO::new)
                .collect(Collectors.toList());
    }

    public List<NodeDTO> getAllNodesFromCluster(String clusterId) throws ExecutionException, InterruptedException {
        return clusterRepository
                .get(clusterId)
                .getNodes()
                .stream()
                .map(n -> nodeMapper.fromNodeToNodeDTO(n))
                .collect(Collectors.toList());
    }
}
