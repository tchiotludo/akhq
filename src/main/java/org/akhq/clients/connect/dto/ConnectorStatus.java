package org.akhq.clients.connect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the response from GET /connectors/{name}/status
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectorStatus {
    private String name;
    private ConnectorState connector;
    private List<TaskState> tasks;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConnectorState {
        private String state;
        @JsonProperty("worker_id")
        private String workerId;
        private String trace;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaskState {
        private int id;
        private String state;
        @JsonProperty("worker_id")
        private String workerId;
        private String trace;
    }
}

