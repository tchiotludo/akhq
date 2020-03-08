package org.akhq;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

@Slf4j
public class KafkaClusterExtension implements BeforeAllCallback, AfterAllCallback {
    private KafkaTestCluster cluster;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // kafka cluster
        KafkaTestCluster.ConnectionString connectionString = KafkaTestCluster.readClusterInfo();

        if (connectionString != null) {
            log.info("Kafka server reused on {}", connectionString.getKafka());
        } else {
            cluster = new KafkaTestCluster(false);
            cluster.run();
        }

    }


    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (cluster != null) {
            cluster.stop();
        }

        cluster = null;
    }
}
