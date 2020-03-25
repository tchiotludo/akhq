package org.kafkahq.service.dto.SchemaRegistry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteSchemaVersionDTO {
    private String clusterId;
    private String subject;
    private int versionId;
}
