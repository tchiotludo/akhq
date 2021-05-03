package org.akhq.repositories;

import com.google.common.collect.ImmutableMap;
import org.akhq.models.Config;
import org.akhq.modules.AbstractKafkaWrapper;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.config.ConfigResource;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ConfigRepository extends AbstractRepository {
    @Inject
    AbstractKafkaWrapper kafkaWrapper;

    public List<Config> findByBroker(String clusterId, Integer name) throws ExecutionException, InterruptedException {
        return this.findByBrokers(clusterId, Collections.singletonList(name)).get(String.valueOf(name));
    }

    public Map<String, List<Config>> findByBrokers(String clusterId, List<Integer> names) throws ExecutionException, InterruptedException {
        return this.find(
            clusterId,
            ConfigResource.Type.BROKER,
            names
                .stream()
                .map(String::valueOf)
                .collect(Collectors.toList())
        );
    }

    public List<Config> findByTopic(String clusterId, String name) throws ExecutionException, InterruptedException {
        return this.findByTopics(clusterId, Collections.singletonList(name)).get(name);
    }

    public Map<String, List<Config>> findByTopics(String clusterId, List<String> names) throws ExecutionException, InterruptedException {
        return this.find(clusterId, ConfigResource.Type.TOPIC, names);
    }

    private Map<String, List<Config>> find(String clusterId, ConfigResource.Type type, List<String> names) throws ExecutionException, InterruptedException {
        Map<String, List<Config>> map = new HashMap<>();

        kafkaWrapper.describeConfigs(clusterId, type, names).forEach((key, value) -> {
            List<Config> collect = value.entries()
                .stream()
                .map(Config::new)
                .sorted(Comparator.comparing(Config::getName))
                .collect(Collectors.toList());

            map.put(key.name(), collect);
        });

        return map;
    }

    public void updateBroker(String clusterId, Integer name, List<Config> configs) throws ExecutionException, InterruptedException {
        this.update(clusterId, ConfigResource.Type.BROKER, String.valueOf(name), configs);
    }

    public void updateTopic(String clusterId, String name, List<Config> configs) throws ExecutionException, InterruptedException {
        this.update(clusterId, ConfigResource.Type.TOPIC, name, configs);
    }

    private void update(String clusterId, ConfigResource.Type type, String name, List<Config> configs) throws ExecutionException, InterruptedException {
        List<ConfigEntry> entries = new ArrayList<>();

        this.find(clusterId, type, Collections.singletonList(name))
            .get(name)
            .stream()
            .filter(config -> config.getSource().name().startsWith("DYNAMIC_"))
            .forEach(config -> entries.add(new ConfigEntry(config.getName(), config.getValue())));

        configs
            .stream()
            .map(config -> new ConfigEntry(config.getName(), config.getValue()))
            .forEach(entries::add);

        kafkaWrapper.alterConfigs(clusterId, ImmutableMap.of(
            new ConfigResource(type, name),
            new org.apache.kafka.clients.admin.Config(entries)
        ));
    }

    public static List<Config> updatedConfigs(Map<String, String> request, List<Config> configs, boolean html) {
        Function<Config, String> configFn = html ?
            (Config config) -> "configs[" + config.getName() + "]" :
            Config::getName;

        return configs
            .stream()
            .filter(config -> !config.isReadOnly())
            .filter(config -> request.containsKey(configFn.apply(config)))
            .filter(config -> {
                String current = config.getValue() == null ? "" : config.getValue();

                return !(current).equals(request.get(configFn.apply(config)));
            })
            .map(config -> config.withValue(request.get(configFn.apply(config))))
            .collect(Collectors.toList());
    }
}
