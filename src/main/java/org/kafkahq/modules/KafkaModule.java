package org.kafkahq.modules;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
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
        props.putAll(this.getConfigProperties("kafka.connections." + clusterId));

        return props;
    }

    private Properties getAdminProperties(String clusterId) {
        Properties props = new Properties();
        props.putAll(this.getConfigProperties("kafka.defaults.admin"));
        props.putAll(this.getConfigProperties("kafka.connections." + clusterId));

        return props;
    }

    private Map<String, AdminClient> adminClient = new HashMap<>();

    public AdminClient getAdminClient(String clusterId) {
        if (!this.adminClient.containsKey(clusterId)) {
            this.adminClient.put(clusterId, AdminClient.create(this.getAdminProperties(clusterId)));
        }

        return this.adminClient.get(clusterId);
    }

    private Map<String, KafkaConsumer<String, String>> consumer = new HashMap<>();

    public KafkaConsumer<String, String> getConsumer(String clusterId) {
        if (!this.consumer.containsKey(clusterId)) {
            this.consumer.put(clusterId, new KafkaConsumer<>(
                this.getConsumerProperties(clusterId),
                new StringDeserializer(),
                new StringDeserializer()
            ));
        }

        return this.consumer.get(clusterId);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(KafkaModule.class).toInstance(new KafkaModule());
    }
}
