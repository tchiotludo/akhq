package org.kafkahq.modules;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jooby.Env;
import org.jooby.Jooby;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class KafkaModule implements Jooby.Module {
    private static Logger logger = LoggerFactory.getLogger(KafkaModule.class);

    @Inject
    private Config config;

    public <T> T debug(Callable<T> task, String format, Object... arguments) throws ExecutionException, InterruptedException {
        long startTime = System.currentTimeMillis();
        T call;

        try {
            call = task.call();
            logger.debug("{} ms -> " + format, (System.currentTimeMillis() - startTime), arguments);
            return call;
        } catch (InterruptedException | ExecutionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Error for " + format, exception);
        }
    }

    public List<String> getClustersList() {
        return this.config
            .getConfig("kafka.connections")
            .entrySet()
            .stream()
            .map(entry -> entry.getKey().split("\\.")[0])
            .distinct()
            .collect(Collectors.toList());
    }

    private Properties getConfigProperties(String path) {
        Properties props = new Properties();

        if (this.config.hasPath(path)) {
            this.config.getConfig(path)
                .entrySet()
                .forEach(config -> props.put(config.getKey(), config.getValue().unwrapped()));
        }

        return props;
    }

    private Properties getConsumerProperties(String clusterId) {
        Properties props = new Properties();
        props.putAll(this.getConfigProperties("kafka.defaults.consumer"));
        props.putAll(this.getConfigProperties("kafka.connections." + clusterId + ".properties"));

        return props;
    }

    private Properties getProducerProperties(String clusterId) {
        Properties props = new Properties();
        props.putAll(this.getConfigProperties("kafka.defaults.producer"));
        props.putAll(this.getConfigProperties("kafka.connections." + clusterId + ".properties"));

        return props;
    }

    private Properties getAdminProperties(String clusterId) {
        Properties props = new Properties();
        props.putAll(this.getConfigProperties("kafka.defaults.admin"));
        props.putAll(this.getConfigProperties("kafka.connections." + clusterId + ".properties"));

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
            if (this.config.hasPath("kafka.connections." + clusterId + ".registry")) {
                this.registryRestClient.put(clusterId, new RestService(
                    this.config.getString("kafka.connections." + clusterId + ".registry")
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

    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(KafkaModule.class).toInstance(new KafkaModule());
    }
}
