package org.kafkahq.repositories;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.micronaut.retry.annotation.Retryable;
import org.kafkahq.models.ConnectDefinition;
import org.kafkahq.models.ConnectPlugin;
import org.kafkahq.modules.KafkaModule;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPluginConfigDefinition;
import org.sourcelab.kafka.connect.apiclient.request.dto.NewConnectorDefinition;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.ConcurrentConfigModificationException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ConnectRepository extends AbstractRepository {
    private KafkaModule kafkaModule;
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Inject
    public ConnectRepository(KafkaModule kafkaModule) {
        this.kafkaModule = kafkaModule;
    }

    @Retryable(includes = ConcurrentConfigModificationException.class, delay = "3s", attempts = "5")
    public ConnectDefinition getDefinition(String clusterId, String name) {
        return new ConnectDefinition(
            this.kafkaModule
                .getConnectRestClient(clusterId)
                .getConnector(name),
            this.kafkaModule
                .getConnectRestClient(clusterId)
                .getConnectorStatus(name)
        );
    }

    @Retryable(includes = ConcurrentConfigModificationException.class, delay = "3s", attempts = "5")
    public List<ConnectDefinition> getDefinitions(String clusterId) {
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .getConnectors()
            .stream()
            .map(s -> getDefinition(clusterId, s))
            .collect(Collectors.toList());
    }


    public Optional<ConnectPlugin> getPlugin(String clusterId, String className) {
        return this.getPlugins(clusterId)
            .stream()
            .filter(connectPlugin -> connectPlugin.getShortClassName().equals(className))
            .findFirst();
    }

    public List<ConnectPlugin> getPlugins(String clusterId) {
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .getConnectorPlugins()
            .stream()
            .map(s -> new ConnectPlugin(
                s,this.kafkaModule
                .getConnectRestClient(clusterId)
                .validateConnectorPluginConfig(new ConnectorPluginConfigDefinition(
                    Iterables.getLast(Arrays.asList(s.getClassName().split("/"))),
                    ImmutableMap.of(
                        "connector.class", s.getClassName(),
                        "topics", "getPlugins"
                    )
                )))
            )
            .collect(Collectors.toList());
    }

    public ConnectDefinition create(String clusterId, String name, Map<String, String> configs) {
        this.kafkaModule
            .getConnectRestClient(clusterId)
            .addConnector(new NewConnectorDefinition(name, configs));

        return getDefinition(clusterId, name);
    }

    public ConnectDefinition update(String clusterId, String name, Map<String, String> configs) {
        this.kafkaModule
            .getConnectRestClient(clusterId)
            .updateConnectorConfig(name, configs);

        return getDefinition(clusterId, name);
    }

    public boolean delete(String clusterId, String name) {
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .deleteConnector(name);
    }

    public boolean pause(String clusterId, String name) {
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .pauseConnector(name);
    }

    public boolean resume(String clusterId, String name) {
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .resumeConnector(name);
    }

    public boolean restart(String clusterId, String name) {
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .restartConnector(name);
    }

    public boolean restartTask(String clusterId, String name, int task) {
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .restartConnectorTask(name, task);
    }

    public static Map<String, String> validConfigs(Map<String, String> configs, String transformsValue) {
        Map<String, String> list = configs
            .entrySet()
            .stream()
            .filter(config -> !config.getKey().equals("transforms-value"))
            .filter(config -> !config.getValue().isEmpty())
            .filter(config -> config.getKey().startsWith("configs["))
            .map(entry -> new AbstractMap.SimpleEntry<>(
                    entry.getKey().substring("configs[".length(), entry.getKey().length() - 1),
                    entry.getValue()
                )
            )
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!transformsValue.trim().isEmpty()) {
            list.putAll(gson.fromJson(
                transformsValue,
                new TypeToken<HashMap<String, String>>() {
                }.getType()
            ));
        }

        return list;
    }
}
