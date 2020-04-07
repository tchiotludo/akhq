package org.akhq.service.dto.connect;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectDefinitionConfigsDTO {
    private ConnectPluginDTO plugin;
    private Map<String, String> configs;
}
