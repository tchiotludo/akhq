package org.kafkahq.service.mapper;
import org.kafkahq.models.Schema;
import org.kafkahq.service.dto.SchemaRegistry.SchemaDTO;
import javax.inject.Singleton;

@Singleton
public class SchemaMapper {

    public SchemaDTO fromSchemaRegistryToSchemaRegistryDTO(Schema schemaRegistry) {

        return new SchemaDTO(schemaRegistry.getId(),schemaRegistry.getSubject(),schemaRegistry.getVersion(),schemaRegistry.getSchema().toString());
    }


