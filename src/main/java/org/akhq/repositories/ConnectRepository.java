package org.akhq.repositories;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.ApplicationContext;
import io.micronaut.retry.annotation.Retryable;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.utils.SecurityService;
import org.akhq.models.ConnectDefinition;
import org.akhq.models.ConnectPlugin;
import org.akhq.modules.KafkaModule;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;
import org.sourcelab.kafka.connect.apiclient.request.dto.*;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.ConcurrentConfigModificationException;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.InvalidRequestException;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.ResourceNotFoundException;

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
    private ApplicationContext applicationContext;

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Retryable(includes = {
        ConcurrentConfigModificationException.class,
        ResourceNotFoundException.class
    }, delay = "3s", attempts = "5")
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

    @Retryable(includes = {
            ConcurrentConfigModificationException.class,
            ResourceNotFoundException.class
    }, delay = "3s", attempts = "5")
    public PagedList<ConnectDefinition> getPaginatedDefinitions (String clusterId, String connectId, Pagination pagination, Optional<String> search, List<String> filters)
            throws IOException, RestClientException, ExecutionException, InterruptedException{
        List<ConnectDefinition> definitions = getDefinitions(clusterId, connectId, search, filters);

        // I'm not sure of how to use the last parameter in this case
        // I look at the implementation for the Schema Registry part, but I don't see how make a similar thing here
        return PagedList.of(definitions, pagination, list -> list);
    }

    public List<ConnectDefinition> getDefinitions(String clusterId, String connectId, Optional<String> search, List<String> filters)
    {
        ConnectorsWithExpandedMetadata unfiltered = this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
            .getConnectorsWithAllExpandedMetadata();

        Collection<ConnectorDefinition> definitions = unfiltered.getAllDefinitions();

        Collection<ConnectorDefinition> connectorsFilteredBySearch =
            definitions.stream().filter(connector -> isSearchMatch(search, connector.getName())
                && isMatchRegex(filters, connector.getName())
        ).collect(Collectors.toList());

        ArrayList<ConnectDefinition> filtered = new ArrayList<>();
        for (ConnectorDefinition item : connectorsFilteredBySearch) {
            if (isMatchRegex(filters, item.getName())) {
                filtered.add(new ConnectDefinition(
                    item,
                    unfiltered.getStatusForConnector(item.getName())
                ));
            }
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
            .filter(connectPlugin -> connectPlugin.getClassName().equals(className))
            .map(s -> mapToConnectPlugin(s, clusterId, connectId, configs))
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
        Map<String,String> config = ImmutableMap.of(
            "connector.class", plugin.getClassName(),
            "topics", "getPlugins"
        );
        return this.mapToConnectPlugin(plugin, clusterId, connectId, config);
    }

    private ConnectPlugin mapToConnectPlugin(ConnectorPlugin plugin, String clusterId, String connectId,
                                                Map<String,String> config) {
        return new ConnectPlugin(
            plugin,
            this.kafkaModule
                .getConnectRestClient(clusterId)
                .get(connectId)
                .validateConnectorPluginConfig(new ConnectorPluginConfigDefinition(
                    Iterables.getLast(Arrays.asList(plugin.getClassName().split("/"))),
                    config
                )));
    }

    private String getShortClassName(String className) {
        String[] split = className.split("\\.");

        return split[split.length - 1];
    }
}
