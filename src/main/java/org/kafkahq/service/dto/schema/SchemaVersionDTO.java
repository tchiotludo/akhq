package org.kafkahq.service.dto.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaVersionDTO {
    private String subject;
    private String compatibilityLevel;
    private int id;
    private int version;
    private String schema;
}
