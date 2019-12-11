package org.kafkahq.repositories;

import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.config.ConfigResource;
import org.kafkahq.modules.AbstractKafkaWrapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class ConfigRepository extends AbstractRepository {
    @Inject
    AbstractKafkaWrapper kafkaWrapper;

    public List<org.kafkahq.models.Config> findByBroker(String clusterId, Integer name) throws ExecutionException, InterruptedException {
        return this.findByBrokers(clusterId, Collections.singletonList(name)).get(String.valueOf(name));
    }

    public Map<String, List<org.kafkahq.models.Config>> findByBrokers(String clusterId, List<Integer> names) throws ExecutionException, InterruptedException {
        return this.find(
            clusterId,
            ConfigResource.Type.BROKER,
            names
                .stream()
                .map(String::valueOf)
                .collect(Collectors.toList())
        );
    }

    public List<org.kafkahq.models.Config> findByTopic(String clusterId, String name) throws ExecutionException, InterruptedException {
        return this.findByTopics(clusterId, Collections.singletonList(name)).get(name);
    }

    public Map<String, List<org.kafkahq.models.Config>> findByTopics(String clusterId, List<String> names) throws ExecutionException, InterruptedException {
        return this.find(clusterId, ConfigResource.Type.TOPIC, names);
    }

    private Map<String, List<org.kafkahq.models.Config>> find(String clusterId, ConfigResource.Type type, List<String> names) throws ExecutionException, InterruptedException {
        Map<String, List<org.kafkahq.models.Config>> map = new HashMap<>();

        kafkaWrapper.describeConfigs(clusterId, type, names).forEach((key, value) -> {
            List<org.kafkahq.models.Config> collect = value.entries()
                .stream()
                .map(org.kafkahq.models.Config::new)
                .sorted(Comparator.comparing(org.kafkahq.models.Config::getName))
                .collect(Collectors.toList());

            map.put(key.name(), collect);
        });

        return map;
    }

    public void updateBroker(String clusterId, Integer name, List<org.kafkahq.models.Config> configs) throws ExecutionException, InterruptedException {
        this.update(clusterId, ConfigResource.Type.BROKER, String.valueOf(name), configs);
    }

    public void updateTopic(String clusterId, String name, List<org.kafkahq.models.Config> configs) throws ExecutionException, InterruptedException {
        this.update(clusterId, ConfigResource.Type.TOPIC, name, configs);
    }

    private void update(String clusterId, ConfigResource.Type type, String name, List<org.kafkahq.models.Config> configs) throws ExecutionException, InterruptedException {
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

    public static List<org.kafkahq.models.Config> updatedConfigs(Map<String, String> request, List<org.kafkahq.models.Config> configs) {
        return configs
            .stream()
            .filter(config -> !config.isReadOnly())
            .filter(config -> request.containsKey("configs[" + config.getName() + "]"))
            .filter(config -> {
                String current = config.getValue() == null ? "" : config.getValue();

                return !(current).equals(request.get("configs[" + config.getName() + "]"));
            })
            .map(config -> config.withValue(request.get("configs[" + config.getName() + "]")))
            .collect(Collectors.toList());
    }
}
