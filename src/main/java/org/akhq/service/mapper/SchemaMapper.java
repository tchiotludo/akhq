package org.akhq.service.mapper;
import org.akhq.models.Schema;
import org.akhq.service.dto.SchemaRegistry.SchemaDTO;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.akhq.service.dto.schema.SchemaVersionDTO;

@Singleton
public class SchemaMapper {

    public SchemaDTO fromSchemaRegistryToSchemaRegistryDTO(Schema schemaRegistry) {

        return new SchemaDTO(schemaRegistry.getId(), schemaRegistry.getSubject(), schemaRegistry.getVersion(), schemaRegistry.getSchema().toString());
    }

    public SchemaVersionDTO fromSchemaToSchemaVersionDTO(Pair<Schema, Schema.Config> schema) {
        return new SchemaVersionDTO(
                schema.getLeft().getSubject(),
                schema.getRight().getCompatibilityLevel().name(),
                schema.getLeft().getSchema().toString()
        );
    }
}
