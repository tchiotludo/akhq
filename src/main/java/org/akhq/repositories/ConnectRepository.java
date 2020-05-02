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
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPlugin;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPluginConfigDefinition;
import org.sourcelab.kafka.connect.apiclient.request.dto.NewConnectorDefinition;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.ConcurrentConfigModificationException;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.InvalidRequestException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ConnectRepository extends AbstractRepository {
    @Inject
    private KafkaModule kafkaModule;

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

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
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
            .getConnectorPlugins()
            .stream()
            .filter(connectPlugin -> getShortClassName(connectPlugin.getClassName()).equals(className))
            .map(s -> mapToConnectPlugin(s, clusterId, connectId))
            .findFirst();
    }

    public List<ConnectPlugin> getPlugins(String clusterId, String connectId) {
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
            .getConnectorPlugins()
            .stream()
            .map(s -> mapToConnectPlugin(s, clusterId, connectId))
            .collect(Collectors.toList());
    }

    public ConnectDefinition create(String clusterId, String connectId, String name, Map<String, String> configs) {
        try {
            this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
                .addConnector(new NewConnectorDefinition(name, configs));
        } catch (InvalidRequestException e) {
            throw new IllegalArgumentException(e);
        }

        return getDefinition(clusterId, connectId, name);
    }

    public ConnectDefinition update(String clusterId, String connectId, String name, Map<String, String> configs) {
        try {
            this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
                .updateConnectorConfig(name, configs);
        } catch (InvalidRequestException e) {
            throw new IllegalArgumentException(e);
        }

        return getDefinition(clusterId, connectId, name);
    }

    public boolean delete(String clusterId, String connectId, String name) {
        try {
            return this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
                .deleteConnector(name);
        } catch (InvalidRequestException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public boolean pause(String clusterId, String connectId, String name) {
        try {
            return this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
                .pauseConnector(name);
        } catch (InvalidRequestException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public boolean resume(String clusterId, String connectId, String name) {
        try {
            return this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
                .resumeConnector(name);
        } catch (InvalidRequestException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public boolean restart(String clusterId, String connectId, String name) {
        try {
            return this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
                .restartConnector(name);
        } catch (InvalidRequestException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public boolean restartTask(String clusterId, String connectId, String name, int task) {
        try {
            return this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
                .restartConnectorTask(name, task);
        } catch (InvalidRequestException e) {
            throw new IllegalArgumentException(e);
        }
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

    private ConnectPlugin mapToConnectPlugin(ConnectorPlugin plugin, String clusterId, String connectId) {
        return new ConnectPlugin(
                plugin,
                this.kafkaModule
                        .getConnectRestClient(clusterId)
                        .get(connectId)
                        .validateConnectorPluginConfig(new ConnectorPluginConfigDefinition(
                                Iterables.getLast(Arrays.asList(plugin.getClassName().split("/"))),
                                ImmutableMap.of(
                                        "connector.class", plugin.getClassName(),
                                        "topics", "getPlugins"
                                )
                        )));
    }

    private String getShortClassName(String className) {
        String[] split = className.split("\\.");

        return split[split.length - 1];
    }
}
