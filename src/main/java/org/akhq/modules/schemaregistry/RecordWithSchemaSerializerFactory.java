package org.akhq.modules.schemaregistry;

import java.io.IOException;

import org.akhq.configs.Connection;
import org.akhq.configs.SchemaRegistryType;
import org.akhq.modules.KafkaModule;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@RequiredArgsConstructor
@Slf4j
public class RecordWithSchemaSerializerFactory {
    private final KafkaModule kafkaModule;

    public SchemaSerializer createSerializer(String clusterId, int schemaId) {
        ParsedSchema parsedSchema = retrieveSchema(clusterId, schemaId);
        SchemaRegistryType schemaRegistryType = getSchemaRegistryType(clusterId);
        return createSerializer(schemaId, parsedSchema, schemaRegistryType);
    }

    public SchemaSerializer createSerializer(int schemaId, ParsedSchema parsedSchema, SchemaRegistryType schemaRegistryType) {
        if (JsonSchemaSerializer.supports(parsedSchema)) {
            return JsonSchemaSerializer.newInstance(schemaId, parsedSchema, schemaRegistryType);
        } if (AvroSerializer.supports(parsedSchema)) {
            return AvroSerializer.newInstance(schemaId, parsedSchema, schemaRegistryType);
        } if (ProtobufSerializer.supports(parsedSchema)) {
            return ProtobufSerializer.newInstance(schemaId, parsedSchema, schemaRegistryType);
        } else {
            String errorMsg = String.format("Schema with id %d has unsupported schema type %s", schemaId, parsedSchema.schemaType());
            throw new IllegalStateException(errorMsg);
        }
    }

    private ParsedSchema retrieveSchema(String clusterId, int schemaId) {
        SchemaRegistryClient registryClient = kafkaModule.getRegistryClient(clusterId);
        try {
            return registryClient.getSchemaById(schemaId);
        } catch (IOException|RestClientException e) {
            String errorMsg = String.format("Can't retrieve schema %d in registry", schemaId);
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    private SchemaRegistryType getSchemaRegistryType(String clusterId) {
        SchemaRegistryType schemaRegistryType = SchemaRegistryType.CONFLUENT;
        Connection.SchemaRegistry schemaRegistry = this.kafkaModule.getConnection(clusterId).getSchemaRegistry();
        if (schemaRegistry != null) {
            schemaRegistryType = schemaRegistry.getType();
        }
        return schemaRegistryType;
    }
}
