package org.kafkahq;

import com.google.common.collect.ImmutableMap;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.PropertySource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.modules.KafkaWrapper;
import org.kafkahq.repositories.AbstractRepository;

import java.util.Optional;

@Slf4j
public class KafkaClusterExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
    private KafkaTestCluster cluster;
    private ApplicationContext applicationContext;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // kafka cluster
        KafkaTestCluster.ConnectionString connectionString = KafkaTestCluster.readClusterInfo();

        if (connectionString != null) {
            log.info("Kafka server reused on {}", connectionString.getKafka());
        } else {
            cluster = new KafkaTestCluster(false);
            cluster.run();
            connectionString = cluster.getClusterInfo();
        }

        // context
        applicationContext = ApplicationContext.run(PropertySource.of(
            "test",
            ImmutableMap.of(
                "kafkahq.connections." + KafkaTestCluster.CLUSTER_ID + ".properties.bootstrap.servers",
                connectionString.getKafka(),
                "kafkahq.connections." + KafkaTestCluster.CLUSTER_ID + ".schema-registry.url",
                connectionString.getSchemaRegistry(),
                "kafkahq.connections." + KafkaTestCluster.CLUSTER_ID + ".connect.url",
                connectionString.getConnect()
            )
        ));
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        final Optional<Object> testInstance = extensionContext.getTestInstance();

        testInstance.ifPresent(o -> {
            if (applicationContext != null) {
                applicationContext.inject(o);

                KafkaModule kafkaModule = applicationContext.getBean(KafkaModule.class);
                AbstractRepository.setWrapper(new KafkaWrapper(kafkaModule, KafkaTestCluster.CLUSTER_ID));
            }
        });
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (cluster != null) {
            cluster.stop();
        }

        if (applicationContext != null) {
            applicationContext.stop();
        }

        cluster = null;
        applicationContext = null;
    }
}
