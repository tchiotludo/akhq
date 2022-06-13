package org.akhq.modules.schemaregistry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

import org.akhq.configs.SchemaRegistryType;

import com.google.protobuf.util.JsonFormat;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ProtobufSerializer implements SchemaSerializer {
    private static final int idSize = 4;
    private final int schemaId;
    private final ProtobufSchema protobufSchema;
    private final SchemaRegistryType schemaRegistryType;

    public static boolean supports(ParsedSchema parsedSchema) {
        return Objects.equals(ProtobufSchema.TYPE, parsedSchema.schemaType());
    }

    public static ProtobufSerializer newInstance(int schemaId, ParsedSchema parsedSchema, SchemaRegistryType schemaRegistryType) {
        if (supports(parsedSchema)) {
            return new ProtobufSerializer(schemaId, (ProtobufSchema) parsedSchema, schemaRegistryType);
        } else {
            String errorMsg = String
                .format("Schema %s has not supported schema type expected %s but found %s", parsedSchema.name(), ProtobufSchema.TYPE,
                    parsedSchema.schemaType());
            throw new IllegalArgumentException(errorMsg);
        }
    }

    @Override
    public byte[] serialize(String json) {
        try {
            return this.fromJsonToProtobuf(json.trim(), protobufSchema, schemaId);
        } catch (IOException e) {
            log.error("Cannot serialize value", e);
            throw new RuntimeException("Cannot serialize value", e);
        }
    }

    private byte[] fromJsonToProtobuf(String json, ProtobufSchema schema, int schemaId) throws IOException {
        log.trace("encoding message {} with schema {} and id {}", json, schema, schemaId);
        final com.google.protobuf.DynamicMessage.Builder messageBuilder = schema.newMessageBuilder();
        JsonFormat.parser().merge(json, messageBuilder);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(schemaRegistryType.getMagicByte());
        outputStream.write(ByteBuffer.allocate(idSize).putInt(schemaId).array());
        // zero index https://stackoverflow.com/questions/64820256/kafka-protobuf-console-consumer-serialization-exception
        outputStream.write((byte) 0x0);
        outputStream.write(messageBuilder.build().toByteArray());
        outputStream.flush();
        outputStream.close();

        return outputStream.toByteArray();
    }

    private ProtobufSerializer(int schemaId, ProtobufSchema protobufSchema, SchemaRegistryType schemaRegistryType) {
        this.schemaId = schemaId;
        this.protobufSchema = protobufSchema;
        this.schemaRegistryType = schemaRegistryType;
    }
}
