package org.akhq.repositories;

import com.google.common.collect.ImmutableMap;
import org.akhq.models.Config;
import org.akhq.modules.AbstractKafkaWrapper;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.config.ConfigResource;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
        List<AlterConfigOp> entries = new ArrayList<>();

        // Get current configs to compare values
        Map<String, String> currentConfigValues = this.find(clusterId, type, Collections.singletonList(name))
            .get(name)
            .stream()
            .collect(Collectors.toMap(Config::getName, config -> config.getValue() != null ? config.getValue() : ""));

        // Add SET operations for configs being updated (only if value actually changed)
        configs.stream()
            .filter(config -> !config.shouldResetToDefault())
            .filter(config -> {
                String currentValue = currentConfigValues.getOrDefault(config.getName(), "");
                String newValue = config.getValue() != null ? config.getValue() : "";
                return !currentValue.equals(newValue);
            })
            .map(config -> new AlterConfigOp(new ConfigEntry(config.getName(), config.getValue()), AlterConfigOp.OpType.SET))
            .forEach(entries::add);

        // Add DELETE operations for configs that should be reset to default
        configs.stream()
            .filter(Config::shouldResetToDefault)
            .map(config -> new AlterConfigOp(new ConfigEntry(config.getName(), null), AlterConfigOp.OpType.DELETE))
            .forEach(entries::add);

        if (entries.isEmpty()) {
            // No actual changes detected - this is fine, just return without doing anything
            return;
        }

        kafkaWrapper.alterConfigs(clusterId, ImmutableMap.of(
            new ConfigResource(type, name),
            entries
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