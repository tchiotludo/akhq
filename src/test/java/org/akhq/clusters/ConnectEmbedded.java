package org.akhq.clusters;

import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.connector.policy.NoneConnectorClientConfigOverridePolicy;
import org.apache.kafka.connect.runtime.Connect;
import org.apache.kafka.connect.runtime.Worker;
import org.apache.kafka.connect.runtime.WorkerConfigTransformer;
import org.apache.kafka.connect.runtime.distributed.DistributedConfig;
import org.apache.kafka.connect.runtime.distributed.DistributedHerder;
import org.apache.kafka.connect.runtime.isolation.Plugins;
import org.apache.kafka.connect.runtime.rest.RestClient;
import org.apache.kafka.connect.runtime.rest.ConnectRestServer;
import org.apache.kafka.connect.storage.*;
import org.apache.kafka.connect.util.TopicAdmin;

import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class ConnectEmbedded {
    private final Connect<DistributedHerder> connect;

    public ConnectEmbedded(final Properties properties) {
        final Time time = Time.SYSTEM;
        Map<String, String> workerProps = effectiveConfigFrom(properties);

        log.debug("Scanning for plugin classes. This might take a moment ...");
        Plugins plugins = new Plugins(workerProps);
        plugins.compareAndSwapWithDelegatingLoader();
        DistributedConfig config = new DistributedConfig(workerProps);

        RestClient restClient = new RestClient(config);

        // Create a TopicAdmin supplier for the backing stores
        Supplier<TopicAdmin> topicAdminSupplier = () -> new TopicAdmin(config.originals());

        // Create RestServer implementation using anonymous class
        ConnectRestServer rest = new ConnectRestServer(1000, restClient, config.originals());
        rest.initializeServer();

        URI advertisedUrl = rest.advertisedUrl();
        String workerId = advertisedUrl.getHost() + ":" + advertisedUrl.getPort();

        // Create offset backing store with new constructor signature
        KafkaOffsetBackingStore offsetBackingStore = new KafkaOffsetBackingStore(
            topicAdminSupplier,
            config::offsetsTopic,
            null  // internalValueConverter - will be set by Worker
        );
        offsetBackingStore.configure(config);

        Worker worker = new Worker(workerId, time, plugins, config, offsetBackingStore, new NoneConnectorClientConfigOverridePolicy());
        WorkerConfigTransformer configTransformer = worker.configTransformer();

        Converter internalValueConverter = worker.getInternalValueConverter();

        // Create status backing store with new constructor signature
        StatusBackingStore statusBackingStore = new KafkaStatusBackingStore(
            time,
            internalValueConverter,
            topicAdminSupplier,
            config.getString(DistributedConfig.STATUS_STORAGE_TOPIC_CONFIG)
        );
        statusBackingStore.configure(config);

        // Create config backing store with new constructor signature
        ConfigBackingStore configBackingStore = new KafkaConfigBackingStore(
            internalValueConverter,
            config,
            configTransformer,
            topicAdminSupplier,
            config.getString(DistributedConfig.CONFIG_TOPIC_CONFIG)
        );

        // Get Kafka cluster ID using local ConnectUtils
        String kafkaClusterId = ConnectUtils.lookupKafkaClusterId(config);

        // Create herder with new constructor signature
        // Kafka 3.9.1 has changed the DistributedHerder constructor
        DistributedHerder herder = new DistributedHerder(
            config,
            time,
            worker,
            kafkaClusterId,
            statusBackingStore,
            configBackingStore,
            advertisedUrl.toString(),
            restClient,
            new NoneConnectorClientConfigOverridePolicy(),
            Collections.emptyList()
        );

        // Initialize resources after creating herder
        rest.initializeResources(herder);

        connect = new Connect<>(herder, rest);
        connect.start();

        log.debug("Startup of embedded Kafka connect at {} completed ...", connect.rest().serverUrl());
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
        return connect.rest().serverUrl().toString();
    }

    public void stop() {
        log.debug("Shutting down embedded connect at {} ...", connect.rest().serverUrl() );

        connect.stop();
        connect.awaitStop();

        log.debug("Shutdown of embedded connect at {} completed", connect.rest().serverUrl());
    }
}
