package org.kafkahq.service;

import org.kafkahq.models.Cluster;
import org.kafkahq.modules.KafkaModule;

import java.util.List;
import java.util.stream.Collectors;

public class ClusterService {

    private KafkaModule kafkaModule;

    public ClusterService(KafkaModule kafkaModule) {
        this.kafkaModule = kafkaModule;
    }

    public List<Cluster> getAllClusters() {
        return kafkaModule
                .getClustersList()
                .stream()
                .map(clusterId -> new Cluster(clusterId))
                .collect(Collectors.toList());
    }
}
