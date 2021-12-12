package org.akhq.repositories;

import org.akhq.models.Cluster;
import org.akhq.modules.AbstractKafkaWrapper;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.ExecutionException;

@Singleton
public class ClusterRepository extends AbstractRepository {
    @Inject
    AbstractKafkaWrapper kafkaWrapper;

    public Cluster get(String clusterId) throws ExecutionException, InterruptedException {
        return new Cluster(kafkaWrapper.describeCluster(clusterId));
    }
}
