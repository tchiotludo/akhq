package org.akhq.mcp.services;

import jakarta.inject.Singleton;
import org.akhq.clients.connect.dto.ConnectorStatus;
import org.akhq.clients.connect.error.ConnectNotFoundException;
import org.akhq.mcp.model.GetConnectorResult;
import org.akhq.mcp.model.GetConnectorStatusResult;
import org.akhq.mcp.model.SearchConnectorsResult;
import org.akhq.models.ConnectDefinition;
import org.akhq.modules.KafkaModule;
import org.akhq.repositories.ConnectRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Singleton
public class AkhqConnectToolService {
    private static final String CONNECTOR_CLASS = "connector.class";

    private final ConnectRepository repository;
    private final KafkaModule kafkaModule;

    public AkhqConnectToolService(ConnectRepository repository, KafkaModule kafkaModule) {
        this.repository = repository;
        this.kafkaModule = kafkaModule;
    }

    public SearchConnectorsResult searchConnectors(
        String cluster, List<String> filters, String connect, String search, String status, Integer maxResults
    ) {
        String connectId = requireConnect(cluster, connect);
        int limit = McpLimits.maxResults(maxResults);
        Optional<String> taskState = Optional.ofNullable(status).map(String::trim).filter(value -> !value.isEmpty());

        List<ConnectDefinition> definitions = repository
            .getDefinitions(cluster, connectId, McpLimits.search(search), taskState, filters).stream()
            .sorted(Comparator.comparing(ConnectDefinition::getName, String::compareToIgnoreCase))
            .toList();
        List<SearchConnectorsResult.ConnectorOverview> returned = definitions.stream()
            .limit(limit)
            .map(definition -> new SearchConnectorsResult.ConnectorOverview(
                definition.getName(),
                definition.getType(),
                connectorClass(definition),
                tasks(definition).size(),
                tasks(definition).stream().collect(Collectors.groupingBy(
                    task -> String.valueOf(task.getState()), TreeMap::new, Collectors.counting()
                ))
            ))
            .toList();
        boolean truncated = definitions.size() > returned.size();

        return new SearchConnectorsResult(
            cluster, connectId, definitions.size(), truncated, returned,
            McpLimits.listMessage("connector", definitions.size(), returned.size(), truncated)
        );
    }

    public GetConnectorResult getConnector(String cluster, String connect, String name) {
        String connectId = requireConnect(cluster, connect);
        ConnectDefinition definition = definition(cluster, connectId, name);

        return new GetConnectorResult(
            cluster, connectId, definition.getName(), definition.getType(),
            connectorClass(definition), tasks(definition).size(), definition.getConfigs()
        );
    }

    public GetConnectorStatusResult getConnectorStatus(String cluster, String connect, String name) {
        String connectId = requireConnect(cluster, connect);
        ConnectorStatus status;
        try {
            status = repository.getStatus(cluster, connectId, name);
        } catch (ConnectNotFoundException e) {
            throw new IllegalArgumentException("Connector '" + name + "' doesn't exist on '" + connectId + "'", e);
        }
        ConnectorStatus.ConnectorState connector = status.getConnector();

        return new GetConnectorStatusResult(
            cluster, connectId, name,
            connector == null ? null : connector.getState(),
            connector == null ? null : connector.getWorkerId(),
            connector == null ? null : McpLimits.truncate(connector.getTrace(), McpLimits.MAX_TRACE_LENGTH),
            Optional.ofNullable(status.getTasks()).orElse(List.of()).stream()
                .map(task -> new GetConnectorStatusResult.TaskStatus(
                    task.getId(), task.getState(), task.getWorkerId(),
                    McpLimits.truncate(task.getTrace(), McpLimits.MAX_TRACE_LENGTH)
                ))
                .toList()
        );
    }

    private ConnectDefinition definition(String cluster, String connectId, String name) {
        try {
            return repository.getDefinition(cluster, connectId, name);
        } catch (ConnectNotFoundException e) {
            throw new IllegalArgumentException("Connector '" + name + "' doesn't exist on '" + connectId + "'", e);
        }
    }

    private String requireConnect(String cluster, String connect) {
        if (connect == null || connect.trim().isEmpty()) {
            throw new IllegalArgumentException("`arguments.connect` is required");
        }
        String connectId = connect.trim();
        Map<String, ?> clients = kafkaModule.getConnectRestClient(cluster);
        if (clients == null || !clients.containsKey(connectId)) {
            throw new IllegalArgumentException(
                "Kafka Connect cluster '" + connectId + "' is not configured on cluster '" + cluster
                    + "'. Use akhq.list_clusters to get the available ones."
            );
        }

        return connectId;
    }

    private static List<ConnectDefinition.TaskDefinition> tasks(ConnectDefinition definition) {
        return Optional.ofNullable(definition.getTasks()).orElse(List.of());
    }

    private static String connectorClass(ConnectDefinition definition) {
        return definition.getConfigs() == null ? null : definition.getConfigs().get(CONNECTOR_CLASS);
    }
}
