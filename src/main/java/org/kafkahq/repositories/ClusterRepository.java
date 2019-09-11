package org.kafkahq.repositories;

import org.kafkahq.models.Cluster;
import org.kafkahq.modules.KafkaWrapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutionException;

@Singleton
public class ClusterRepository extends AbstractRepository {
    @Inject
    KafkaWrapper kafkaWrapper;

    public Cluster get(String clusterId) throws ExecutionException, InterruptedException {
        return new Cluster(kafkaWrapper.describeCluster(clusterId));
    }
}
