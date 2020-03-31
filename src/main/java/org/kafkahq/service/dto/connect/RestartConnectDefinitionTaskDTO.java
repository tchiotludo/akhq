package org.kafkahq.service.dto.connect;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestartConnectDefinitionTaskDTO {
    private String clusterId;
    private String connectId;
    private String definitionId;
    private int taskId;
}
