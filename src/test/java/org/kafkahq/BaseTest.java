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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BaseTest {
    protected static Logger logger = LoggerFactory.getLogger(RecordRepository.class);
    protected static App app;
    private static KafkaTestCluster cluster;

    @BeforeClass
    public static void setup() throws IOException {
        // kafka cluster
        String kafkaConnectString;
        Path path = Paths.get(System.getProperty("java.io.tmpdir"), "/kafkahq-cs.txt");

        if (Files.exists(path)) {
            kafkaConnectString = new String(Files.readAllBytes(path));
            logger.info("Kafka server reused on {}", kafkaConnectString);
        } else {
            cluster = new KafkaTestCluster((short) 1, false);
            cluster.run();
            kafkaConnectString = cluster.getCluster().getKafkaConnectString();
        }

        // app
        app = new App();
        app
            .use(ConfigFactory
                .empty()
                .withValue(
                    "kafka.connections." + KafkaTestCluster.CLUSTER_ID + ".bootstrap.servers",
                    ConfigValueFactory.fromAnyRef(kafkaConnectString)
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
    public static void tearDown() {
        app.stop();
        app = null;

        if (cluster != null) {
            cluster.stop();
            cluster = null;
        }
    }
}