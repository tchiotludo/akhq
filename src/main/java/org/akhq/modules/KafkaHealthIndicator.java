package org.akhq.modules;

import io.micronaut.health.HealthStatus;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.management.health.indicator.HealthResult;
import io.micronaut.management.health.indicator.annotation.Readiness;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Map;

@Singleton
@Readiness
public class KafkaHealthIndicator implements HealthIndicator {

    @Override
    public Publisher<HealthResult> getResult() {
        // Simulate Kafka being unreachable
        boolean kafkaIsHealthy = false;

        if (kafkaIsHealthy) {
            return Mono.just(HealthResult.builder("kafka").status(HealthStatus.UP).build());
        } else {
            return Mono.just(
                HealthResult.builder("kafka")
                    .status(HealthStatus.DOWN)
                    .details(Map.of("error", "Simulated Kafka failure"))
                    .build()
            );
        }
    }
}
