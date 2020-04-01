package org.akhq.service.dto.connect;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteConnectDefinitionDTO {
    private String clusterId;
    private String connectId;
    private String definitionId;
}
