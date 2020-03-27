package org.kafkahq.service.dto.connect;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModifyConnectDefinitionStateDTO {
    private String clusterId;
    private String connectId;
    private String definitionId;
}
