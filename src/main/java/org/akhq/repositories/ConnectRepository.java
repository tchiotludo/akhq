package org.akhq.repositories;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.micronaut.context.ApplicationContext;
import io.micronaut.retry.annotation.Retryable;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.utils.SecurityService;
import org.akhq.configs.SecurityProperties;
import org.akhq.models.ClusterStats;
import org.akhq.models.ConnectDefinition;
import org.akhq.models.ConnectPlugin;
import org.akhq.modules.KafkaModule;
import org.akhq.utils.UserGroupUtils;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPlugin;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPluginConfigDefinition;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorStatus;
import org.sourcelab.kafka.connect.apiclient.request.dto.NewConnectorDefinition;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.ConcurrentConfigModificationException;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.InvalidRequestException;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.ResourceNotFoundException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Singleton
public class ConnectRepository extends AbstractRepository {
    @Inject
    private KafkaModule kafkaModule;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private UserGroupUtils userGroupUtils;

    @Inject
    private SecurityProperties securityProperties;

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Retryable(includes = {
        ConcurrentConfigModificationException.class,
        ResourceNotFoundException.class
    }, delay = "3s", attempts = "5")
    public ConnectDefinition getDefinition(String clusterId, String connectId, String name) {
        if (!isMatchRegex(getConnectFilterRegex(), name)) {
            throw new IllegalArgumentException(String.format("Not allowed to view Connector %s", name));
        }
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
    public List<ConnectDefinition> getDefinitions(String clusterId, String connectId) {
        Collection<String> unfiltered = this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
            .getConnectors();

        ArrayList<String> filtered = new ArrayList<String>();
        for (String item : unfiltered) {
            if (isMatchRegex(getConnectFilterRegex(), item)) {
                filtered.add(item);
            }
        }

        return filtered.stream()
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

    public ClusterStats.ConnectStats getConnectStats(String clusterId, String connectId) {
        KafkaConnectClient client = this.kafkaModule.getConnectRestClient(clusterId).get(connectId);
        int connectorCount = 0;
        int tasks = 0;
        Map<String, Integer> stateCount = new HashMap<>();
        Collection<String> connectors = client.getConnectors();
        connectorCount += connectors.size();
        for (String c : connectors) {
            tasks += client.getConnectorTasks(c).size();
            List<ConnectorStatus.TaskStatus> statuses = client.getConnectorStatus(c).getTasks();
            statuses.stream()
                    .map(ConnectorStatus.TaskStatus::getState)
                    //  --> List("RUNNING","RUNNING","PAUSED")
                    .collect(Collectors.groupingBy(w -> w))
                    // --> Map("RUNNING": List("RUNNING","RUNNING"),
                    //         "PAUSED": List("PAUSED")
                    .forEach((k, v) -> {
                        if (stateCount.containsKey(k)) {
                            stateCount.computeIfPresent(k, (y,z) -> z + v.size());
                        } else {
                            stateCount.put(k, v.size());
                        }
                    });
//            statuses.stream().collect(Collectors.groupingBy(ConnectorStatus.TaskStatus::getState, () -> stateCount, Collectors.counting()));
        }
        return new ClusterStats.ConnectStats(connectId, connectorCount, tasks, stateCount);
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

    private Optional<List<String>> getConnectFilterRegex() {

        List<String> connectFilterRegex = new ArrayList<>();

        if (applicationContext.containsBean(SecurityService.class)) {
            SecurityService securityService = applicationContext.getBean(SecurityService.class);
            Optional<Authentication> authentication = securityService.getAuthentication();
            if (authentication.isPresent()) {
                Authentication auth = authentication.get();
                connectFilterRegex.addAll(getConnectFilterRegexFromAttributes(auth.getAttributes()));
            }
        }
        // get Connect filter regex for default groups
        connectFilterRegex.addAll(getConnectFilterRegexFromAttributes(
            userGroupUtils.getUserAttributes(Collections.singletonList(securityProperties.getDefaultGroup()))
        ));

        return Optional.of(connectFilterRegex);
    }

    @SuppressWarnings("unchecked")
    private List<String> getConnectFilterRegexFromAttributes(Map<String, Object> attributes) {
        if (attributes.get("connects-filter-regexp") != null) {
            if (attributes.get("connects-filter-regexp") instanceof List) {
                return (List<String>)attributes.get("connects-filter-regexp");
            }
        }
        return new ArrayList<>();
    }
}
