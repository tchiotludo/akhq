package org.akhq;

import com.google.common.collect.ImmutableMap;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.type.Argument;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.akhq.utils.ResultNextList;
import org.akhq.utils.ResultPagedList;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

// https://github.com/micronaut-projects/micronaut-test/commit/0f32d13876decfc33f3f94238e280552422bf170#diff-985f52f80183621fbb0bc4f031044158R16
// https://github.com/micronaut-projects/micronaut-test/issues/32
@MicronautTest(propertySources = "application.yml")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(KafkaClusterExtension.class)
abstract public class AbstractTest implements TestPropertyProvider {
    @Inject
    @Client("/")
    protected RxHttpClient client;

    protected <I, O> List<O> retrieveList(MutableHttpRequest<I> request, Class<O> bodyType) {
        return client.toBlocking().retrieve(
            request.basicAuth("admin", "pass"),
            Argument.listOf(bodyType)
        );
    }

    @SuppressWarnings("unchecked")
    protected <I, O> ResultPagedList<O> retrievePagedList(MutableHttpRequest<I> request, Class<O> bodyType) {
        return client.toBlocking().retrieve(
            request.basicAuth("admin", "pass"),
            Argument.of(ResultPagedList.class, bodyType)
        );
    }

    @SuppressWarnings("unchecked")
    protected <I, O> ResultNextList<O> retrieveNextList(MutableHttpRequest<I> request, Class<O> bodyType) {
        return client.toBlocking().retrieve(
            request.basicAuth("admin", "pass"),
            Argument.of(ResultNextList.class, bodyType)
        );
    }

    protected <I> String retrieve(MutableHttpRequest<I> request) {
        return client.toBlocking().retrieve(
            request.basicAuth("admin", "pass")
        );
    }

    protected <I, O> O retrieve(MutableHttpRequest<I> request, Class<O> bodyType) {
        return client.toBlocking().retrieve(
            request.basicAuth("admin", "pass"),
            bodyType
        );
    }

    protected <I> void exchange(MutableHttpRequest<I> request) {
        client.toBlocking().exchange(
            request.basicAuth("admin", "pass")
        );
    }

    @NonNull
    @Override
    public Map<String, String> getProperties() {
        KafkaTestCluster.ConnectionString connectionString = null;

        try {
            connectionString = KafkaTestCluster.readClusterInfo();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ImmutableMap.<String, String>builder()
                .put("akhq.connections." + KafkaTestCluster.CLUSTER_ID + ".properties.bootstrap.servers",
                    connectionString.getKafka())
                .put("akhq.connections." + KafkaTestCluster.CLUSTER_ID + ".schema-registry.url",
                    connectionString.getSchemaRegistry())
                .put("akhq.connections." + KafkaTestCluster.CLUSTER_ID + ".connect[0].name",
                    "connect-1")
                .put("akhq.connections." + KafkaTestCluster.CLUSTER_ID + ".connect[0].url",
                    connectionString.getConnect1())
                .put("akhq.connections." + KafkaTestCluster.CLUSTER_ID + ".connect[1].name",
                    "connect-2")
                .put("akhq.connections." + KafkaTestCluster.CLUSTER_ID + ".connect[1].url",
                    connectionString.getConnect2())
                .build();
    }
}
