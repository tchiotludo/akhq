package org.akhq.service.dto.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaDTO {

    private String cluster;
    private String subject;
    private String schema;
    private String compatibilityLevel;

}
