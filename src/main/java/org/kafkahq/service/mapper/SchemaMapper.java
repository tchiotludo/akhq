package org.kafkahq.service.mapper;

import org.apache.commons.lang3.tuple.Pair;
import org.kafkahq.models.Schema;
import org.kafkahq.service.dto.schema.SchemaVersionDTO;

import javax.inject.Singleton;

@Singleton
public class SchemaMapper {
    public SchemaVersionDTO fromSchemaToSchemaVersionDTO(Pair<Schema, Schema.Config> schema) {
        return new SchemaVersionDTO(
                schema.getLeft().getSubject(),
                schema.getRight().getCompatibilityLevel().name(),
                schema.getLeft().getSchema().toString()
        );
    }
}
