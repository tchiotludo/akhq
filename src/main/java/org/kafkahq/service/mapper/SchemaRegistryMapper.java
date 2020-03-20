package org.kafkahq.service.mapper;

import org.kafkahq.configs.Connection;
import org.kafkahq.models.ConsumerGroup;
import org.kafkahq.models.Schema;
import org.kafkahq.service.dto.ConsumerGroup.ConsumerGroupDTO;
import org.kafkahq.service.dto.SchemaRegistry.SchemaRegistryDTO;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class SchemaRegistryMapper {

    public SchemaRegistryDTO fromSchemaRegistryToSchemaRegistryDTO(Schema schemaRegistry) {

        return new SchemaRegistryDTO(schemaRegistry.getId(),schemaRegistry.getSubject(),schemaRegistry.getVersion(),schemaRegistry.getSchema(),schemaRegistry.getException());
    }
}
