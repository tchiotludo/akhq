package org.kafkahq.repositories;

import org.kafkahq.models.Cluster;

import javax.inject.Singleton;
import java.util.concurrent.ExecutionException;

@Singleton
public class ClusterRepository extends AbstractRepository {
    public Cluster get() throws ExecutionException, InterruptedException {
        return new Cluster(kafkaWrapper.describeCluster());
    }
}
