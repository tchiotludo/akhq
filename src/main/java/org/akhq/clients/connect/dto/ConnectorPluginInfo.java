package org.akhq.clients.connect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents one entry from GET /connector-plugins
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectorPluginInfo {
    @JsonProperty("class")
    private String className;
    private String type;
    private String version;
}

