package org.kafkahq.repositories;

import com.google.inject.Binder;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.apache.kafka.common.config.ConfigResource;
import org.jooby.Env;
import org.jooby.Jooby;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class ConfigRepository extends AbstractRepository implements Jooby.Module {
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

    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(ConfigRepository.class).toInstance(new ConfigRepository());
    }
}
