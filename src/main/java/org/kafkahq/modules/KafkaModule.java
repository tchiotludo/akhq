package org.kafkahq.modules;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.kafkahq.configs.AbstractProperties;
import org.kafkahq.configs.Connection;
import org.kafkahq.configs.Default;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class KafkaModule {
    @Inject
    private List<Connection> connections;

    @Inject
    private List<Default> defaults;

    public <T> T debug(Callable<T> task, String format, Object... arguments) throws ExecutionException, InterruptedException {
        long startTime = System.currentTimeMillis();
        T call;

        try {
            call = task.call();
            log.debug("{} ms -> " + format, (System.currentTimeMillis() - startTime), arguments);
            return call;
        } catch (InterruptedException | ExecutionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Error for " + format, exception);
        }
    }

    public List<String> getClustersList() {
        return this.connections
            .stream()
            .map(r -> r.getName().split("\\.")[0])
            .distinct()
            .collect(Collectors.toList());
    }

    private Connection getConnection(String cluster) {
        return this.connections
            .stream()
            .filter(r -> r.getName().equals(cluster))
            .findFirst()
            .get();
    }

    private Properties getDefaultsProperties(List<? extends AbstractProperties> current, String type) {
        Properties properties = new Properties();

        current
            .stream()
            .filter(r -> r.getName().equals(type))
            .forEach(r -> r.getProperties()
                .forEach(properties::put)
            );

        return properties;
    }

    private Properties getConsumerProperties(String clusterId) {
        Properties props = new Properties();
        props.putAll(this.getDefaultsProperties(this.defaults, "consumer"));
        props.putAll(this.getDefaultsProperties(this.connections, clusterId));

        return props;
    }

    private Properties getProducerProperties(String clusterId) {
        Properties props = new Properties();
        props.putAll(this.getDefaultsProperties(this.defaults, "producer"));
        props.putAll(this.getDefaultsProperties(this.connections, clusterId));

        return props;
    }

    private Properties getAdminProperties(String clusterId) {
        Properties props = new Properties();
        props.putAll(this.getDefaultsProperties(this.defaults, "admin"));
        props.putAll(this.getDefaultsProperties(this.connections, clusterId));

        return props;
    }

    private Map<String, AdminClient> adminClient = new HashMap<>();

    public AdminClient getAdminClient(String clusterId) {
        if (!this.adminClient.containsKey(clusterId)) {
            this.adminClient.put(clusterId, AdminClient.create(this.getAdminProperties(clusterId)));
        }

        return this.adminClient.get(clusterId);
    }

    private Map<String, KafkaConsumer<byte[], byte[]>> consumers = new HashMap<>();

    public KafkaConsumer<byte[], byte[]> getConsumer(String clusterId) {
        if (!this.consumers.containsKey(clusterId)) {
            this.consumers.put(clusterId, new KafkaConsumer<>(
                this.getConsumerProperties(clusterId),
                new ByteArrayDeserializer(),
                new ByteArrayDeserializer()
            ));
        }

        return this.consumers.get(clusterId);
    }

    public KafkaConsumer<byte[], byte[]> getConsumer(String clusterId, Properties properties) {
        Properties props = this.getConsumerProperties(clusterId);
        props.putAll(properties);

        return new KafkaConsumer<>(
            props,
            new ByteArrayDeserializer(),
            new ByteArrayDeserializer()
        );
    }

    private Map<String, KafkaProducer<String, String>> producers = new HashMap<>();

    public KafkaProducer<String, String> getProducer(String clusterId) {
        if (!this.producers.containsKey(clusterId)) {
            this.producers.put(clusterId, new KafkaProducer<>(
                this.getProducerProperties(clusterId),
                new StringSerializer(),
                new StringSerializer()
            ));
        }

        return this.producers.get(clusterId);
    }

    private Map<String, RestService> registryRestClient = new HashMap<>();

    public RestService getRegistryRestClient(String clusterId) {
        if (!this.registryRestClient.containsKey(clusterId)) {
            Connection connection = this.getConnection(clusterId);

            if (connection.getSchemaRegistry().isPresent()) {
                this.registryRestClient.put(clusterId, new RestService(
                    connection.getSchemaRegistry().get().toString()
                ));
            }
        }

        return this.registryRestClient.get(clusterId);
    }

    public SchemaRegistryClient getRegistryClient(String clusterId) {
        return new CachedSchemaRegistryClient(
            this.getRegistryRestClient(clusterId),
            Integer.MAX_VALUE
        );
    }
}
