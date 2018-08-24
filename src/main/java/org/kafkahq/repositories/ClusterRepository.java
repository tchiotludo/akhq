package org.kafkahq.repositories;

import com.google.inject.Binder;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.jooby.Env;
import org.jooby.Jooby;
import org.kafkahq.models.Cluster;

import java.util.concurrent.ExecutionException;

@Singleton
public class ClusterRepository extends AbstractRepository implements Jooby.Module {
    public Cluster get() throws ExecutionException, InterruptedException {
        return new Cluster(kafkaWrapper.describeCluster());
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(ClusterRepository.class).toInstance(new ClusterRepository());
    }
}
