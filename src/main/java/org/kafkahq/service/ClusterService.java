package org.kafkahq.service;

import org.kafkahq.models.Cluster;
import org.kafkahq.modules.KafkaModule;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ClusterService {

    private KafkaModule kafkaModule;

    @Inject
    public ClusterService(KafkaModule kafkaModule) {
        this.kafkaModule = kafkaModule;
    }

    public List<Cluster> getAllClusters() {
        return kafkaModule
                .getClustersList()
                .stream()
                .map(Cluster::new)
                .collect(Collectors.toList());
    }
}
