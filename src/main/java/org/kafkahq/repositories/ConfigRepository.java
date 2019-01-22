package org.kafkahq.repositories;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.config.ConfigResource;
import org.jooby.Env;
import org.jooby.Jooby;
import org.kafkahq.modules.KafkaModule;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class ConfigRepository extends AbstractRepository implements Jooby.Module {
    @Inject
    private KafkaModule kafkaModule;

    public List<org.kafkahq.models.Config> findByBroker(String name) throws ExecutionException, InterruptedException {
        return this.findByBrokers(Collections.singletonList(name)).get(name);
    }

    public Map<String, List<org.kafkahq.models.Config>> findByBrokers(List<String> names) throws ExecutionException, InterruptedException {
        return this.find(ConfigResource.Type.BROKER, names);
    }

    public List<org.kafkahq.models.Config> findByTopic(String name) throws ExecutionException, InterruptedException {
        return this.findByTopics(Collections.singletonList(name)).get(name);
    }

    public Map<String, List<org.kafkahq.models.Config>> findByTopics(List<String> names) throws ExecutionException, InterruptedException {
        return this.find(ConfigResource.Type.TOPIC, names);
    }

    private Map<String, List<org.kafkahq.models.Config>> find(ConfigResource.Type type, List<String> names) throws ExecutionException, InterruptedException {
        Map<String, List<org.kafkahq.models.Config>> map = new HashMap<>();

        kafkaWrapper.describeConfigs(type, names).forEach((key, value) -> {
            List<org.kafkahq.models.Config> collect = value.entries()
                .stream()
                .map(org.kafkahq.models.Config::new)
                .sorted(Comparator.comparing(org.kafkahq.models.Config::getName))
                .collect(Collectors.toList());

            map.put(key.name(), collect);
        });

        return map;
    }

    public void updateBroker(String clusterId, String name, List<org.kafkahq.models.Config> configs) throws ExecutionException, InterruptedException {
        this.update(clusterId, ConfigResource.Type.BROKER, name, configs);
    }

    public void updateTopic(String clusterId, String name, List<org.kafkahq.models.Config> configs) throws ExecutionException, InterruptedException {
        this.update(clusterId, ConfigResource.Type.TOPIC, name, configs);
    }

    private void update(String clusterId, ConfigResource.Type type, String name, List<org.kafkahq.models.Config> configs) throws ExecutionException, InterruptedException {
        List<ConfigEntry> entries = configs
            .stream()
            .map(config -> new ConfigEntry(config.getName(), config.getValue()))
            .collect(Collectors.toList());

        kafkaModule.getAdminClient(clusterId)
            .alterConfigs(ImmutableMap.of(
                new ConfigResource(type, name),
                new org.apache.kafka.clients.admin.Config(entries)
            ))
            .all()
            .get();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(ConfigRepository.class).toInstance(new ConfigRepository());
    }
}
