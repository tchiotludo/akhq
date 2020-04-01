package org.akhq.service.dto.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSchemaDTO {
    private String clusterId;
    private String subject;
    private String compatibilityLevel;
    private String schema;
}
