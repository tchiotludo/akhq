package org.kafkahq.clusters;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.runtime.Connect;
import org.apache.kafka.connect.runtime.HerderProvider;
import org.apache.kafka.connect.runtime.Worker;
import org.apache.kafka.connect.runtime.WorkerConfigTransformer;
import org.apache.kafka.connect.runtime.distributed.DistributedConfig;
import org.apache.kafka.connect.runtime.distributed.DistributedHerder;
import org.apache.kafka.connect.runtime.isolation.Plugins;
import org.apache.kafka.connect.runtime.rest.RestServer;
import org.apache.kafka.connect.storage.*;
import org.apache.kafka.connect.util.ConnectUtils;

import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class ConnectEmbedded {
    private final Connect connect;

    public ConnectEmbedded(final Properties properties) {
        final Time time = Time.SYSTEM;
        Map<String, String> workerProps = effectiveConfigFrom(properties);

        log.debug("Scanning for plugin classes. This might take a moment ...");
        Plugins plugins = new Plugins(workerProps);
        plugins.compareAndSwapWithDelegatingLoader();
        DistributedConfig config = new DistributedConfig(workerProps);


        RestServer rest = new RestServer(config);
        HerderProvider provider = new HerderProvider();
        rest.start(provider, plugins);

        URI advertisedUrl = rest.advertisedUrl();
        String workerId = advertisedUrl.getHost() + ":" + advertisedUrl.getPort();

        KafkaOffsetBackingStore offsetBackingStore = new KafkaOffsetBackingStore();
        offsetBackingStore.configure(config);

        Worker worker = new Worker(workerId, time, plugins, config, offsetBackingStore);
        WorkerConfigTransformer configTransformer = worker.configTransformer();

        Converter internalValueConverter = worker.getInternalValueConverter();
        StatusBackingStore statusBackingStore = new KafkaStatusBackingStore(time, internalValueConverter);
        statusBackingStore.configure(config);

        ConfigBackingStore configBackingStore = new KafkaConfigBackingStore(
            internalValueConverter,
            config,
            configTransformer);

        DistributedHerder herder = new DistributedHerder(
            config,
            time,
            worker,
            ConnectUtils.lookupKafkaClusterId(config),
            statusBackingStore,
            configBackingStore,
            advertisedUrl.toString()
        );

        connect = new Connect(herder, rest);
        connect.start();

        provider.setHerder(herder);

        log.debug("Startup of embedded Kafka connect at {} completed ...", connect.restUrl());
    }

    private Map<String, String> effectiveConfigFrom(final Properties initialConfig) {
        final Properties effectiveConfig = new Properties();
        effectiveConfig.put("offset.flush.interval.ms", "10000");
        effectiveConfig.put("rest.host.name", "127.0.0.1");

        effectiveConfig.putAll(initialConfig);

        return effectiveConfig.entrySet().stream().collect(
            Collectors.toMap(
                e -> e.getKey().toString(),
                e -> e.getValue().toString()
            )
        );
    }

    public String connectUrl() {
        return connect.restUrl().toString();
    }

    public void stop() {
        log.debug("Shutting down embedded connect at {} ...", connect.restUrl() );

        connect.stop();
        connect.awaitStop();

        log.debug("Shutdown of embedded connect at {} completed", connect.restUrl());
    }
}