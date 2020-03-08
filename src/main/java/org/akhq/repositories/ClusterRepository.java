package org.akhq.repositories;

import org.akhq.models.Cluster;
import org.akhq.modules.AbstractKafkaWrapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutionException;

@Singleton
public class ClusterRepository extends AbstractRepository {
    @Inject
    AbstractKafkaWrapper kafkaWrapper;

    public Cluster get(String clusterId) throws ExecutionException, InterruptedException {
        return new Cluster(kafkaWrapper.describeCluster(clusterId));
    }
}
