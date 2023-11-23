package org.akhq.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorDefinition;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorStatus;

import java.util.*;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class ConnectDefinition {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private String name;
    private String type;
    private Map<String, String> configs;
    private List<TaskDefinition> tasks;

    public ConnectDefinition(ConnectorDefinition connectorDefinition, ConnectorStatus connectorStatus) {
        this.name = connectorDefinition.getName();
        this.type = connectorDefinition.getType();
        this.configs = connectorDefinition.getConfig();
        this.tasks = connectorDefinition.getTasks()
            .stream()
            .map(r -> connectorStatus
                .getTasks()
                .stream()
                .filter(taskStatus -> taskStatus.getId() == r.getTask())
                .findFirst()
                .map(s ->
                    new TaskDefinition(r, s)
                )
                .orElse(null)
            )
            .filter(Objects::nonNull)
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

    @JsonIgnore
    public String getConfigsAsJson() {
        return GSON.toJson(this.getConfigs());
    }

    @JsonIgnore
    public String getTransformConfigsAsJson() {
        return GSON.toJson(this.getConfigs()
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
    @NoArgsConstructor
    public static final class TaskDefinition {
        private String connector;
        private int id;
        private String state;
        private String workerId;
        private String trace;

        public TaskDefinition(ConnectorDefinition.TaskDefinition taskDefinition, ConnectorStatus.TaskStatus status) {
            this.connector = taskDefinition.getConnector();
            this.id = taskDefinition.getTask();
            this.state = status.getState();
            this.workerId = status.getWorkerId();
            this.trace = status.getTrace();
        }
    }
}
