package org.akhq.clusters;

import io.confluent.ksql.function.InternalFunctionRegistry;
import io.confluent.ksql.function.MutableFunctionRegistry;
import io.confluent.ksql.function.UserFunctionLoader;
import io.confluent.ksql.metrics.MetricCollectors;
import io.confluent.ksql.rest.server.KsqlRestApplication;
import io.confluent.ksql.rest.server.KsqlRestConfig;
import io.confluent.ksql.rest.server.state.ServerState;
import io.confluent.ksql.util.KsqlConfig;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class KsqlDBEmbedded {
    private final KsqlRestApplication ksqlRestApplication;
    private final String listeners;

    public KsqlDBEmbedded(final Properties properties) {
        Map<String, String> restAppProps = effectiveConfigFrom(properties);

        final MetricCollectors metricCollectors = new MetricCollectors();
        final ServerState serverState = new ServerState();
        final KsqlRestConfig ksqlRestConfig = new KsqlRestConfig(restAppProps);
        final MutableFunctionRegistry functionRegistry = new InternalFunctionRegistry();
        final KsqlConfig ksqlConfig = new KsqlConfig(ksqlRestConfig.getKsqlConfigProperties());
        final String ksqlInstallDir = ksqlRestConfig.getString(KsqlRestConfig.INSTALL_DIR_CONFIG);
        UserFunctionLoader.newInstance(
            ksqlConfig,
            functionRegistry,
            ksqlInstallDir,
            metricCollectors.getMetrics()
        ).load();
        ksqlRestApplication = KsqlRestApplication.buildApplication(
            ksqlRestConfig,
            serverState,
            metricCollectors,
            functionRegistry,
            Instant.now()
        );

        listeners = (String) properties.get("listeners");

        ksqlRestApplication.startAsync();

        log.debug("Startup of embedded Kafka KsqlDB at {} completed ...", listeners);
    }

    private Map<String, String> effectiveConfigFrom(final Properties initialConfig) {
        final Properties effectiveConfig = new Properties();
        effectiveConfig.put("ksql.service.id", "ksql");

        effectiveConfig.putAll(initialConfig);

        return effectiveConfig.entrySet().stream().collect(
            Collectors.toMap(
                e -> e.getKey().toString(),
                e -> e.getValue().toString()
            )
        );
    }

    public String ksqlDbUrl() {
        return listeners;
    }

    public void stop() throws InterruptedException {
        log.debug("Shutting down embedded KsqlDB at {} ...", listeners);

        ksqlRestApplication.shutdown();

        log.debug("Shutdown of embedded KsqlDB at {} completed", listeners);
    }
}
