package org.akhq.service.mapper;

import org.akhq.models.Schema;
import org.akhq.service.dto.SchemaRegistry.SchemaDTO;

import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.akhq.service.dto.schema.SchemaVersionDTO;

import java.util.Optional;

@Singleton
public class SchemaMapper {

    public SchemaDTO fromSchemaRegistryToSchemaRegistryDTO(Schema schemaRegistry) {

        return new SchemaDTO(schemaRegistry.getId(), schemaRegistry.getSubject(), schemaRegistry.getVersion(), schemaRegistry.getSchema().toString());
    }

    public SchemaVersionDTO fromSchemaToSchemaVersionDTO(Pair<Schema, Optional<String>> schema) {
        return new SchemaVersionDTO(
                schema.getLeft().getSubject(),
                schema.getRight().orElse(""),
                schema.getLeft().getId(),
                schema.getLeft().getVersion(),
                schema.getLeft().getSchema().toString()
        );
    }
}
