package org.kafkahq.metrics;

import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics;
import io.github.mweirauch.micrometer.jvm.extras.ProcessThreadMetrics;
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
@Requires(property = MICRONAUT_METRICS_BINDERS + ".jvm-extras.enabled", value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
public class JvmExtrasMetrics {
    @Bean
    @Primary
    @Singleton
    public ProcessMemoryMetrics processMemoryMetrics() {
        return new ProcessMemoryMetrics();
    }

    @Bean
    @Primary
    @Singleton
    public ProcessThreadMetrics processThreadMetrics() {
        return new ProcessThreadMetrics();
    }
}
