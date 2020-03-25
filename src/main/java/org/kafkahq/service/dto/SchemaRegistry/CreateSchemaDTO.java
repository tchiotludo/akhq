package org.kafkahq.service.dto.SchemaRegistry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSchemaDTO {
    private String cluster;
    private String subject;
    private String compatibilityLevel;
    private String schema;
}
