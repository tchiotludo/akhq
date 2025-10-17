package org.akhq.clusters;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class KafkaEmbedded {
    private static final String DEFAULT_KAFKA_IMAGE = "confluentinc/cp-kafka:8.0.0";
    private KafkaContainer kafka;

    public KafkaEmbedded() {
        kafka = new KafkaContainer(DockerImageName.parse(DEFAULT_KAFKA_IMAGE))
            .withEnv("KAFKA_PROCESS_ROLES", "broker");
        log.debug("Starting embedded Kafka broker using Testcontainers...");
        kafka.start();
        log.debug("Startup of embedded Kafka broker at {} completed.", brokerList());
    }

    public String brokerList() {
        return kafka.getBootstrapServers();
    }

    public void stop() {
        log.debug("Shutting down embedded Kafka broker at {} ...", brokerList());
        kafka.stop();
        log.debug("Shutdown of embedded Kafka broker at {} completed.", brokerList());
    }
}
