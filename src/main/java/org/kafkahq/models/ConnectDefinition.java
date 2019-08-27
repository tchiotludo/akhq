package org.kafkahq.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorDefinition;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorStatus;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode
@Getter
public class ConnectDefinition {
    private final String name;
    private final String type;
    private final Map<String, String> configs;
    private final List<TaskDefinition> tasks;
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ConnectDefinition(ConnectorDefinition connectorDefinition, ConnectorStatus connectorStatus) {
        this.name = connectorDefinition.getName();
        this.type = connectorDefinition.getType();
        this.configs = connectorDefinition.getConfig();
        this.tasks = connectorDefinition.getTasks()
            .stream()
            .map(r -> new TaskDefinition(
                r,
                connectorStatus
                    .getTasks()
                    .stream()
                    .filter(taskStatus -> taskStatus.getId() == r.getTask())
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException("Task '" + r.getTask() + "' doesn't exist"))
            ))
            .collect(Collectors.toList());
    }

    public String getShortClassName() {
        String[] split = this.getConfigs()
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey().equals("connector.class"))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Couldn't find connector.class config"))
            .getValue()
                .split("\\.")
        ;
        return split[split.length - 1];
    }

    public String getConfigsAsJson() {
        return gson.toJson(this.getConfigs());
    }

    public String getTransformConfigsAsJson() {
        return gson.toJson(this.getConfigs()
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey().startsWith("transforms."))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    public Optional<String> getConfig(String config) {
        return this.configs.containsKey(config) ? Optional.of(this.configs.get(config)) : Optional.empty();
    }

    public boolean isPaused() {
        return this.getTasks().stream().anyMatch(taskDefinition -> taskDefinition.getState().equals("PAUSED"));
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    public static final class TaskDefinition {
        private final String connector;
        private final int id;
        private final String state;
        private final String workerId;
        private final String trace;

        public TaskDefinition(ConnectorDefinition.TaskDefinition taskDefinition, ConnectorStatus.TaskStatus status) {
            this.connector = taskDefinition.getConnector();
            this.id = taskDefinition.getTask();
            this.state = status.getState();
            this.workerId = status.getWorkerId();
            this.trace = status.getTrace();
        }
    }
}
