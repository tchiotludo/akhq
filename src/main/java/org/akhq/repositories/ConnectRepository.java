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
import org.akhq.configs.SecurityProperties;
import org.akhq.models.ConnectDefinition;
import org.akhq.models.ConnectPlugin;
import org.akhq.modules.KafkaModule;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;
import org.akhq.utils.UserGroupUtils;
import org.sourcelab.kafka.connect.apiclient.request.dto.*;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.ConcurrentConfigModificationException;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.InvalidRequestException;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.ResourceNotFoundException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
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
    public PagedList<ConnectDefinition> getPaginatedDefinitions (String clusterId, String connectId, Pagination pagination, Optional<String> search)
            throws IOException, RestClientException, ExecutionException, InterruptedException{
        List<ConnectDefinition> definitions = getDefinitions(clusterId, connectId, search);

        // I'm not sure of how to use the last parameter in this case
        // I look at the implementation for the Schema Registry part, but I don't see how make a similar thing here
        return PagedList.of(definitions, pagination, list -> list);
    }

    public List<ConnectDefinition> getDefinitions(String clusterId, String connectId, Optional<String> search
    )
    {
        ConnectorsWithExpandedMetadata unfiltered = this.kafkaModule
            .getConnectRestClient(clusterId)
            .get(connectId)
            .getConnectorsWithAllExpandedMetadata();

        Collection<ConnectorDefinition> definitions = unfiltered.getAllDefinitions();

        Collection<ConnectorDefinition> connectorsFilteredBySearch = search.map(
                query -> definitions.stream().filter(connector -> connector.getName().contains(query))
        ).orElse(definitions.stream()).collect(Collectors.toList());

        ArrayList<ConnectDefinition> filtered = new ArrayList<>();
        for (ConnectorDefinition item : connectorsFilteredBySearch) {
            if (isMatchRegex(getConnectFilterRegex(), item.getName())) {
                filtered.add(new ConnectDefinition(
                    item,
                    unfiltered.getStatusForConnector(item.getName())
                ));
            }
        }

        return filtered;
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
        if (attributes.get("connectsFilterRegexp") != null) {
            if (attributes.get("connectsFilterRegexp") instanceof List) {
                return (List<String>)attributes.get("connectsFilterRegexp");
            }
        }
        return new ArrayList<>();
    }
}
