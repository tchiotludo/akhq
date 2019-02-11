package org.kafkahq;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.apache.curator.test.InstanceSpec;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.modules.KafkaWrapper;
import org.kafkahq.repositories.AbstractRepository;
import org.kafkahq.repositories.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BaseTest {
    protected static Logger logger = LoggerFactory.getLogger(RecordRepository.class);
    protected static App app;
    private static KafkaTestCluster cluster;

    @BeforeClass
    public static void setup() throws Exception {
        // kafka cluster
        KafkaTestCluster.ConnectionString connectionString = KafkaTestCluster.readClusterInfo();

        if (connectionString != null) {
            logger.info("Kafka server reused on {}", connectionString.getKafka());
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