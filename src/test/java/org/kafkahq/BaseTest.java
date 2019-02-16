package org.kafkahq;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.test.InstanceSpec;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.modules.KafkaWrapper;
import org.kafkahq.repositories.AbstractRepository;

@Slf4j
public class BaseTest {
    protected static App app;
    private static KafkaTestCluster cluster;

    @BeforeClass
    public static void setup() throws Exception {
        // kafka cluster
        KafkaTestCluster.ConnectionString connectionString = KafkaTestCluster.readClusterInfo();

        if (connectionString != null) {
            log.info("Kafka server reused on {}", connectionString.getKafka());
        } else {
            cluster = new KafkaTestCluster(false);
            cluster.run();
            connectionString = cluster.getClusterInfo();
        }

        // app
        app = new App();
        app
            .use(ConfigFactory
                .empty()
                .withValue(
                    "kafka.connections." + KafkaTestCluster.CLUSTER_ID + ".properties.bootstrap.servers",
                    ConfigValueFactory.fromAnyRef(connectionString.getKafka())
                )
                .withValue(
                    "kafka.connections." + KafkaTestCluster.CLUSTER_ID + ".registry",
                    ConfigValueFactory.fromAnyRef(connectionString.getSchemaRegistry())
                )
                .withValue(
                    "application.port",
                    ConfigValueFactory.fromAnyRef(String.valueOf(InstanceSpec.getRandomPort()))
                )
                .withFallback(ConfigFactory.load("application"))
            )
            .start("test");
        AbstractRepository.setWrapper(new KafkaWrapper(app.require(KafkaModule.class), KafkaTestCluster.CLUSTER_ID));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        app.stop();
        app = null;

        if (cluster != null) {
            cluster.stop();
            cluster = null;
        }
    }
}