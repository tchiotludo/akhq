package org.akhq.repositories;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.micronaut.retry.annotation.Retryable;
import org.akhq.models.ConnectDefinition;
import org.akhq.models.ConnectPlugin;
import org.akhq.modules.KafkaModule;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPluginConfigDefinition;
import org.sourcelab.kafka.connect.apiclient.request.dto.NewConnectorDefinition;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.ConcurrentConfigModificationException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ConnectRepository extends AbstractRepository {
    @Inject
    private KafkaModule kafkaModule;

    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Retryable(includes = ConcurrentConfigModificationException.class, delay = "3s", attempts = "5")
    public ConnectDefinition getDefinition(String clusterId, String connectId, String name) {
        return new ConnectDefinition(
            this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
                .getConnector(name),
            this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
                .getConnectorStatus(name)
        );
    }

    @Retryable(includes = ConcurrentConfigModificationException.class, delay = "3s", attempts = "5")
    public List<ConnectDefinition> getDefinitions(String clusterId, String connectId) {
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
            .getConnectors()
            .stream()
            .map(s -> getDefinition(clusterId, connectId, s))
            .collect(Collectors.toList());
    }


    public Optional<ConnectPlugin> getPlugin(String clusterId, String connectId, String className) {
        return this.getPlugins(clusterId, connectId)
            .stream()
            .filter(connectPlugin -> connectPlugin.getShortClassName().equals(className))
            .findFirst();
    }

    public List<ConnectPlugin> getPlugins(String clusterId, String connectId) {
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
            .getConnectorPlugins()
            .stream()
            .map(s -> new ConnectPlugin(
                s,this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
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

    public ConnectDefinition create(String clusterId, String connectId, String name, Map<String, String> configs) {
        this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
            .addConnector(new NewConnectorDefinition(name, configs));

        return getDefinition(clusterId, connectId, name);
    }

    public ConnectDefinition update(String clusterId, String connectId, String name, Map<String, String> configs) {
        this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
            .updateConnectorConfig(name, configs);

        return getDefinition(clusterId, connectId, name);
    }

    public boolean delete(String clusterId, String connectId, String name) {
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
            .deleteConnector(name);
    }

    public boolean pause(String clusterId, String connectId, String name) {
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
            .pauseConnector(name);
    }

    public boolean resume(String clusterId, String connectId, String name) {
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
            .resumeConnector(name);
    }

    public boolean restart(String clusterId, String connectId, String name) {
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
            .restartConnector(name);
    }

    public boolean restartTask(String clusterId, String connectId, String name, int task) {
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
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
