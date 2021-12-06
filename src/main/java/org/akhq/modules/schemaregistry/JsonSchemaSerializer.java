package org.akhq.modules.schemaregistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.serializers.json.AbstractKafkaJsonSchemaSerializer;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.SchemaRegistryType;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
public class JsonSchemaSerializer extends AbstractKafkaJsonSchemaSerializer<String> implements SchemaSerializer {
    private final int schemaId;
    private final JsonSchema jsonSchema;
    private final SchemaRegistryType schemaRegistryType;

    public static JsonSchemaSerializer newInstance(int schemaId, ParsedSchema parsedSchema, SchemaRegistryType schemaRegistryType) {
        if (supports(parsedSchema)) {
            return new JsonSchemaSerializer(schemaId, (JsonSchema) parsedSchema, schemaRegistryType);
        }
        String errorMsg = String.format("Schema %s has not supported schema type expected %s but found %s", parsedSchema.name(), JsonSchema.TYPE, parsedSchema.schemaType());
        throw new IllegalArgumentException(errorMsg);
    }

    @Override
    public byte[] serialize(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            jsonSchema.validate(jsonObject);
        } catch (JsonProcessingException e) {
            String errorMsg = String.format("Provided json [%s] is not valid according to schema", json);
            log.error(errorMsg);
            throw new RuntimeException(errorMsg, e);
        } catch (ValidationException e) {
            String validationErrorMsg = String.format(
                "Provided json message is not valid according to jsonSchema (id=%d): %s",
                schemaId,
                e.getMessage()
            );
            throw new IllegalArgumentException(validationErrorMsg);
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(schemaRegistryType.getMagicByte());
            out.write(ByteBuffer.allocate(idSize).putInt(schemaId).array());
            out.write(json.getBytes(StandardCharsets.UTF_8));
            byte[] bytes = out.toByteArray();
            out.close();
            return bytes;
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not serialize json [%s]", json), e);
        }
    }

    public static boolean supports(ParsedSchema parsedSchema) {
        return Objects.equals(JsonSchema.TYPE, parsedSchema.schemaType());
    }

    private JsonSchemaSerializer(int schemaId, JsonSchema jsonSchema, SchemaRegistryType schemaRegistryType) {
        this.schemaId = schemaId;
        this.jsonSchema = jsonSchema;
        this.schemaRegistryType = schemaRegistryType;
    }
}
