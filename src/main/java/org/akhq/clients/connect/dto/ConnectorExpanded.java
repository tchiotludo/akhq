package org.akhq.clients.connect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents an expanded entry from GET /connectors?expand=info and/or expand=status
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectorExpanded {
    private ConnectorInfo info;
    private ConnectorStatus status;
}

