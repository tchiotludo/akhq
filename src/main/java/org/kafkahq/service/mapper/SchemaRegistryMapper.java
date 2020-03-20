package org.kafkahq.service.mapper;
import org.kafkahq.models.Schema;
import org.kafkahq.service.dto.SchemaRegistry.SchemaRegistryDTO;
import javax.inject.Singleton;

@Singleton
public class SchemaRegistryMapper {

    public SchemaRegistryDTO fromSchemaRegistryToSchemaRegistryDTO(Schema schemaRegistry) {

        return new SchemaRegistryDTO(schemaRegistry.getId(),schemaRegistry.getSubject(),schemaRegistry.getVersion(),schemaRegistry.getSchema().toString());
    }
}
