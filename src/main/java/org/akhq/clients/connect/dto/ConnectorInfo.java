package org.akhq.clients.connect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents the response from GET /connectors/{name}
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectorInfo {
    private String name;
    private String type;
    private Map<String, String> config;
    private List<TaskRef> tasks;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaskRef {
        private String connector;
        private int task;
    }
}

