package org.akhq.repositories;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.ApplicationContext;
import io.micronaut.retry.annotation.Retryable;
import org.akhq.models.ConnectDefinition;
import org.akhq.models.ConnectPlugin;
import org.akhq.models.audit.ConnectAuditEvent;
import org.akhq.modules.AuditModule;
import org.akhq.modules.KafkaModule;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;
import org.akhq.clients.connect.dto.ConnectorExpanded;
import org.akhq.clients.connect.dto.ConnectorPluginInfo;
import org.akhq.clients.connect.dto.ConnectorPluginValidation;
import org.akhq.clients.connect.error.ConnectBadRequestException;
import org.akhq.clients.connect.error.ConnectConflictException;
import org.akhq.clients.connect.error.ConnectNotFoundException;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class ConnectRepository extends AbstractRepository {
    @Inject
    private KafkaModule kafkaModule;

    @Inject
    private AuditModule auditModule;

    @Inject
    private ApplicationContext applicationContext;

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Retryable(includes = {
        ConnectConflictException.class,
        ConnectNotFoundException.class,
        ConnectBadRequestException.class
    }, delay = "3s", attempts = "5")
    public ConnectDefinition getDefinition(String clusterId, String connectId, String name) {
        var client = this.kafkaModule.getConnectRestClient(clusterId).get(connectId);
        return new ConnectDefinition(
            client.getConnector(name),
            client.getConnectorStatus(name)
        );
    }

    @Retryable(includes = {
        ConnectConflictException.class,
        ConnectNotFoundException.class,
        ConnectBadRequestException.class
    }, delay = "3s", attempts = "5")
    public PagedList<ConnectDefinition> getPaginatedDefinitions (String clusterId, String connectId, Pagination pagination, Optional<String> search, Optional<String> status, List<String> filters)
            throws IOException, RestClientException, ExecutionException, InterruptedException{
        List<ConnectDefinition> definitions = getDefinitions(clusterId, connectId, search, status, filters);

        // I'm not sure of how to use the last parameter in this case
        // I look at the implementation for the Schema Registry part, but I don't see how make a similar thing here
        return PagedList.of(definitions, pagination, list -> list);
    }

    public List<ConnectDefinition> getDefinitions(String clusterId, String connectId, Optional<String> search, Optional<String> status, List<String> filters) {
        Map<String, ConnectorExpanded> expanded = this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
            .getConnectorsExpanded();

        List<ConnectDefinition> filtered = expanded.entrySet().stream()
            .filter(e -> isSearchMatch(search, e.getKey()) && isMatchRegex(filters, e.getKey()))
            .map(e -> new ConnectDefinition(e.getValue().getInfo(), e.getValue().getStatus()))
            .collect(Collectors.toList());

        if (status.isPresent() && !status.get().isEmpty()) {
            filtered.removeIf(def -> def.getTasks().stream().noneMatch(
                task -> task.getState().equalsIgnoreCase(status.get())
            ));
        }

        return filtered;
    }

    public Optional<ConnectPlugin> validatePlugin(String clusterId, String connectId, String className,
                                                  Map<String, String> configs) {
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
            .getConnectorPlugins()
            .stream()
            .filter(p -> p.getClassName().equals(className))
            .map(p -> mapToConnectPlugin(p, clusterId, connectId, configs))
            .findFirst();
    }

    public List<ConnectPlugin> getPlugins(String clusterId, String connectId) {
        return this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
            .getConnectorPlugins()
            .stream()
            .map(p -> mapToConnectPlugin(p, clusterId, connectId))
            .collect(Collectors.toList());
    }

    public ConnectDefinition create(String clusterId, String connectId, String name, Map<String, String> configs) {
        try {
            this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
                .createConnector(name, configs);
        } catch (ConnectBadRequestException e) {
            throw new IllegalArgumentException(e);
        }

        auditModule.save(ConnectAuditEvent.newConnector(clusterId, connectId, name));
        return getDefinition(clusterId, connectId, name);
    }

    @Retryable(includes = {
        ConnectConflictException.class,
        ConnectNotFoundException.class,
        ConnectBadRequestException.class
    }, delay = "3s", attempts = "5")
    public ConnectDefinition update(String clusterId, String connectId, String name, Map<String, String> configs) {
        try {
            this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
                .updateConnectorConfig(name, configs);
        } catch (ConnectBadRequestException e) {
            throw new IllegalArgumentException(e);
        }

        auditModule.save(ConnectAuditEvent.updateConnector(clusterId, connectId, name));
        return getDefinition(clusterId, connectId, name);
    }

    public void delete(String clusterId, String connectId, String name) {
        try {
            this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
                .deleteConnector(name);
            auditModule.save(ConnectAuditEvent.deleteConnector(clusterId, connectId, name));
        } catch (ConnectBadRequestException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void pause(String clusterId, String connectId, String name) {
        try {
            this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
                .pauseConnector(name);
            auditModule.save(ConnectAuditEvent.pauseConnector(clusterId, connectId, name));
        } catch (ConnectBadRequestException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void resume(String clusterId, String connectId, String name) {
        try {
            this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
                .resumeConnector(name);
            auditModule.save(ConnectAuditEvent.resumeConnector(clusterId, connectId, name));
        } catch (ConnectBadRequestException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public boolean restart(String clusterId, String connectId, String name) {
        try {
            this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
                .restartConnector(name);
            auditModule.save(ConnectAuditEvent.restartConnector(clusterId, connectId, name));
            return true;
        } catch (ConnectBadRequestException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void restartTask(String clusterId, String connectId, String name, int task) {
        try {
            this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
                .restartConnectorTask(name, task);
            auditModule.save(ConnectAuditEvent.restartTaskConnector(clusterId, connectId, name, task));
        } catch (ConnectBadRequestException e) {
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

    private ConnectPlugin mapToConnectPlugin(ConnectorPluginInfo plugin, String clusterId, String connectId) {
        Map<String, String> config = ImmutableMap.of(
            "connector.class", plugin.getClassName(),
            "topics", "getPlugins"
        );
        return this.mapToConnectPlugin(plugin, clusterId, connectId, config);
    }

    private ConnectPlugin mapToConnectPlugin(ConnectorPluginInfo plugin, String clusterId, String connectId,
                                             Map<String, String> config) {
        String shortName = Iterables.getLast(Arrays.asList(plugin.getClassName().split("\\.")));
        ConnectorPluginValidation validation = this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
            .validateConnectorPluginConfig(shortName, config);
        return new ConnectPlugin(plugin, validation);
    }

    private String getShortClassName(String className) {
        String[] split = className.split("\\.");

        return split[split.length - 1];
    }
}
