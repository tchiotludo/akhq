package org.akhq.modules.schemaregistry;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.SchemaRegistryType;
import org.apache.avro.Conversions;
import org.apache.avro.Schema;
import org.apache.avro.data.TimeConversions;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.*;

import jakarta.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

@Singleton
@Slf4j
public class AvroSerializer implements SchemaSerializer {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        .registerModule(new JavaTimeModule())
        .registerModule(new Jdk8Module())
        .setTimeZone(TimeZone.getDefault());
    private static final TypeReference<Map<String, Object>> TYPE_REFERENCE = new TypeReference<>() {};

    public static final int SCHEMA_ID_SIZE = 4;
    private final int schemaId;
    private final AvroSchema avroSchema;
    private final SchemaRegistryType schemaRegistryType;

    public static boolean supports(ParsedSchema parsedSchema) {
        return Objects.equals(AvroSchema.TYPE, parsedSchema.schemaType());
    }

    public static AvroSerializer newInstance(int schemaId, ParsedSchema parsedSchema, SchemaRegistryType schemaRegistryType) {
        if (supports(parsedSchema)) {
            return new AvroSerializer(schemaId, (AvroSchema) parsedSchema, schemaRegistryType);
        } else {
            String errorMsg = String.format("Schema %s has not supported schema type expected %s but found %s", parsedSchema.name(), AvroSchema.TYPE, parsedSchema.schemaType());
            throw new IllegalArgumentException(errorMsg);
        }
    }

    @Override
    public byte[] serialize(String json) {
        try {
            return this.fromJsonToAvro(json.trim(), avroSchema.rawSchema(), schemaId);
        } catch (IOException e) {
            log.error("Cannot serialize value", e);
            throw new RuntimeException("Cannot serialize value", e);
        }
    }

    private byte[] fromJsonToAvro(String json, Schema schema, int schemaId) throws IOException {
        log.trace("encoding message {} with schema {} and id {}", json, schema, schemaId);

        Object genericRecord;
        try {
            Map<String, Object> map = MAPPER.readValue(json, TYPE_REFERENCE);
            genericRecord = org.akhq.utils.AvroSerializer.recordSerializer(map, schema);
        } catch (IOException e) {
            // rollback to jsonDecoder to handle special schema like "string"
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

        GenericDatumWriter<Object> w = new GenericDatumWriter<>(schema, genericData);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(schemaRegistryType.getMagicByte());
        outputStream.write(ByteBuffer.allocate(SCHEMA_ID_SIZE).putInt(schemaId).array());

        Encoder e = EncoderFactory.get().binaryEncoder(outputStream, null);

        w.write(genericRecord, e);
        e.flush();

        return outputStream.toByteArray();
    }

    private AvroSerializer(int schemaId, AvroSchema avroSchema, SchemaRegistryType schemaRegistryType) {
        this.schemaId = schemaId;
        this.avroSchema = avroSchema;
        this.schemaRegistryType = schemaRegistryType;
    }
}
