package org.kafkahq.metrics;

import io.micrometer.core.instrument.binder.kafka.KafkaConsumerMetrics;
import io.micronaut.configuration.metrics.annotation.RequiresMetrics;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;

import javax.inject.Singleton;

import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_BINDERS;

@Factory
@RequiresMetrics
@Requires(property = MICRONAUT_METRICS_BINDERS + ".kafka.enabled", value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
public class KafkaMetrics {
    @Bean
    @Primary
    @Singleton
    public KafkaConsumerMetrics processKafkaConsumerMetrics() {
        return new KafkaConsumerMetrics();
    }
}
