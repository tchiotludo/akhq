package org.kafkahq.service.dto.node;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigOperationDTO {

    private String clusterId;
    private Integer nodeId;
    private Map<String, String> configs;
}
