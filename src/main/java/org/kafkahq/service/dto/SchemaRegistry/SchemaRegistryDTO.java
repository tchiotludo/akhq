package org.kafkahq.service.dto.SchemaRegistry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaRegistryDTO {

    private int id;
    private String subject;
    private int version;
    private org.apache.avro.Schema schema;
    private Exception exception;
}
