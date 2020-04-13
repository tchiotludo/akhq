package org.akhq.service;

import org.akhq.modules.KafkaModule;
import org.akhq.repositories.ClusterRepository;
import org.akhq.service.dto.ClusterDTO;

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

//    public List<NodeDTO> getAllNodesFromCluster(String clusterId) throws ExecutionException, InterruptedException {
//        return clusterRepository
//                .get(clusterId)
//                .getNodes()
//                .stream()
//                .map(n -> nodeMapper.fromNodeToNodeDTO(n))
//                .collect(Collectors.toList());
//    }
}
