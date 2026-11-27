package org.akhq.modules.schemaregistry.apicurio;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.json.jackson.Jackson;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import lombok.extern.slf4j.Slf4j;
import org.akhq.modules.schemaregistry.SchemaSerializer;
import org.apache.avro.Conversions;
import org.apache.avro.Schema;
import org.apache.avro.data.TimeConversions;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.everit.json.schema.ValidationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TimeZone;

/**
 * Serializes JSON input into Apicurio Registry Kafka wire format for Avro, JSON Schema,
 * and Protobuf schemas. Supports both 4-byte (Apicurio v3 default) and 8-byte (legacy) IDs,
 * as well as Apicurio Protobuf {@code Ref} and Confluent message-index framing.
 */
@Slf4j
public class ApicurioSchemaSerializer implements SchemaSerializer {
    private static final ObjectMapper AVRO_MAPPER = new ObjectMapper()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
        .registerModule(new JavaTimeModule())
        .registerModule(new Jdk8Module())
        .setTimeZone(TimeZone.getDefault());
    private static final ObjectMapper JSON_SCHEMA_MAPPER = Jackson.newObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final long schemaId;
    private final ParsedSchema parsedSchema;
    private final ApicurioConfig config;

    public ApicurioSchemaSerializer(long schemaId, ParsedSchema parsedSchema, ApicurioConfig config) {
        if (parsedSchema == null) {
            throw new IllegalArgumentException("ParsedSchema cannot be null for schema id " + schemaId);
        }
        this.schemaId = schemaId;
        this.parsedSchema = parsedSchema;
        this.config = config;
    }

    @Override
    public byte[] serialize(String json) {
        String trimmed = json.trim();
        return switch (parsedSchema.schemaType()) {
            case AvroSchema.TYPE -> serializeAvro(trimmed, ((AvroSchema) parsedSchema).rawSchema());
            case JsonSchema.TYPE -> serializeJson(trimmed, (JsonSchema) parsedSchema);
            case ProtobufSchema.TYPE -> serializeProtobuf(trimmed, (ProtobufSchema) parsedSchema);
            default -> throw new IllegalStateException(
                String.format("Schema with id %d has unsupported schema type %s", schemaId, parsedSchema.schemaType())
            );
        };
    }

    private byte[] serializeAvro(String json, Schema schema) {
        try {
            Object genericRecord;
            try {
                Map<String, Object> map = AVRO_MAPPER.readValue(json, MAP_TYPE);
                genericRecord = org.akhq.utils.AvroSerializer.recordSerializer(map, schema);
            } catch (IOException e) {
                DatumReader<Object> reader = new GenericDatumReader<>(schema);
                Decoder decoder = DecoderFactory.get().jsonDecoder(schema, json);
                genericRecord = reader.read(null, decoder);
            }

            GenericData genericData = new GenericData();
            genericData.addLogicalTypeConversion(new Conversions.UUIDConversion());
            genericData.addLogicalTypeConversion(new Conversions.DecimalConversion());
            genericData.addLogicalTypeConversion(new TimeConversions.DateConversion());
            genericData.addLogicalTypeConversion(new TimeConversions.TimeMicrosConversion());
            genericData.addLogicalTypeConversion(new TimeConversions.TimeMillisConversion());
            genericData.addLogicalTypeConversion(new TimeConversions.TimestampMicrosConversion());
            genericData.addLogicalTypeConversion(new TimeConversions.TimestampMillisConversion());

            GenericDatumWriter<Object> writer = new GenericDatumWriter<>(schema, genericData);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeWireHeader(out);
            Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(genericRecord, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Cannot serialize Avro value for Apicurio schema id {}", schemaId, e);
            throw new RuntimeException("Cannot serialize value", e);
        }
    }

    private byte[] serializeJson(String json, JsonSchema jsonSchema) {
        try {
            jsonSchema.validate(JSON_SCHEMA_MAPPER.readTree(json));
        } catch (JsonProcessingException e) {
            String errorMsg = String.format("Provided json [%s] is not valid according to schema", json);
            log.error(errorMsg);
            throw new RuntimeException(errorMsg, e);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(String.format(
                "Provided json message is not valid according to jsonSchema (id=%d): %s",
                schemaId,
                e.getMessage()
            ));
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeWireHeader(out);
            out.write(json.getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not serialize json [%s]", json), e);
        }
    }

    private byte[] serializeProtobuf(String json, ProtobufSchema protoSchema) {
        try {
            DynamicMessage.Builder builder = protoSchema.newMessageBuilder();
            JsonFormat.parser().merge(json, builder);
            DynamicMessage message = builder.build();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeWireHeader(out);

            if (config.isProtobufSendTypeRef()) {
                writeApicurioProtobufRef(message.getDescriptorForType().getName(), out);
            }
            if (config.isProtobufSendIndexes()) {
                // Default index 0 for first message type (matches Confluent MessageIndexes encoding)
                out.write((byte) 0x00);
            }

            message.writeTo(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Cannot serialize Protobuf value for Apicurio schema id {}", schemaId, e);
            throw new RuntimeException("Cannot serialize value", e);
        }
    }

    private void writeWireHeader(ByteArrayOutputStream out) throws IOException {
        out.write(ApicurioRecordDeserializer.MAGIC_BYTE);
        int idSize = config.getWriteIdSize();
        if (idSize == 8) {
            out.write(ByteBuffer.allocate(8).putLong(schemaId).array());
        } else {
            out.write(ByteBuffer.allocate(4).putInt((int) schemaId).array());
        }
    }

    static void writeApicurioProtobufRef(String messageName, ByteArrayOutputStream out) throws IOException {
        int refSize = CodedOutputStream.computeStringSize(1, messageName);
        CodedOutputStream cos = CodedOutputStream.newInstance(out);
        cos.writeUInt32NoTag(refSize);
        cos.writeString(1, messageName);
        cos.flush();
    }
}
