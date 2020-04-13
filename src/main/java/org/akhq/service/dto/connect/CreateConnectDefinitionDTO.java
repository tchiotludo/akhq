package org.akhq.service.dto.connect;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateConnectDefinitionDTO {
    private String clusterId;
    private String connectId;
    private String name;
    private Map<String, String> configs;
    private String transformsValue;
}
