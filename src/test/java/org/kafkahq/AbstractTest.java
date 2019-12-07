package org.kafkahq;

import com.google.common.collect.ImmutableMap;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;

// https://github.com/micronaut-projects/micronaut-test/commit/0f32d13876decfc33f3f94238e280552422bf170#diff-985f52f80183621fbb0bc4f031044158R16
// https://github.com/micronaut-projects/micronaut-test/issues/32
@MicronautTest(propertySources = "application.yml")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(KafkaClusterExtension.class)
abstract public class AbstractTest implements TestPropertyProvider {
    @Nonnull
    @Override
    public Map<String, String> getProperties() {
        KafkaTestCluster.ConnectionString connectionString = null;

        try {
            connectionString = KafkaTestCluster.readClusterInfo();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ImmutableMap.of(
            "kafkahq.connections." + KafkaTestCluster.CLUSTER_ID + ".properties.bootstrap.servers",
            connectionString.getKafka(),
            "kafkahq.connections." + KafkaTestCluster.CLUSTER_ID + ".schema-registry.url",
            connectionString.getSchemaRegistry(),
            "kafkahq.connections." + KafkaTestCluster.CLUSTER_ID + ".connect.url",
            connectionString.getConnect()
        );
    }
}
