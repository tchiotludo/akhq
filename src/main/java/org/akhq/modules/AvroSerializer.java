package org.akhq.modules;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.SchemaRegistryType;
import org.apache.avro.Conversions;
import org.apache.avro.Schema;
import org.apache.avro.data.TimeConversions;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.akhq.configs.Connection;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TimeZone;

@Singleton
@Slf4j
public class AvroSerializer {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        .registerModule(new JavaTimeModule())
        .registerModule(new Jdk8Module())
        .setTimeZone(TimeZone.getDefault());
    private static final TypeReference<Map<String, Object>> TYPE_REFERENCE = new TypeReference<>() {};

    private final int MAGIC_BYTE;

    public static final int SCHEMA_ID_SIZE = 4;

    private final SchemaRegistryClient registryClient;

    public AvroSerializer(SchemaRegistryClient registryClient, SchemaRegistryType schemaRegistryType) {
        this.registryClient = registryClient;

        if (schemaRegistryType == SchemaRegistryType.TIBCO) {
            MAGIC_BYTE = (byte) 0x80;
        } else {
            MAGIC_BYTE = 0x0;
        }
    }

    public byte[] toAvro(String json, int schemaId) {
        byte[] asBytes;
        try {
            Schema schema = this.registryClient.getById(schemaId);
            asBytes = this.fromJsonToAvro(json.trim(), schema, schemaId);
        } catch (IOException | RestClientException e) {
            throw new RuntimeException(String.format("Can't retrieve schema %d in registry", schemaId), e);
        }
        return asBytes;
    }

    private byte[] fromJsonToAvro(String json, Schema schema, int schemaId) throws IOException {
        log.trace("encoding message {} with schema {} and id {}", json, schema, schemaId);

        Map<String, Object> map = MAPPER.readValue(json, TYPE_REFERENCE);
        GenericRecord genericRecord = org.akhq.utils.AvroSerializer.recordSerializer(map, schema);

        GenericData genericData = new GenericData();
        genericData.addLogicalTypeConversion(new Conversions.UUIDConversion());
        genericData.addLogicalTypeConversion(new Conversions.DecimalConversion());
        genericData.addLogicalTypeConversion(new TimeConversions.DateConversion());
        genericData.addLogicalTypeConversion(new TimeConversions.TimeMicrosConversion());
        genericData.addLogicalTypeConversion(new TimeConversions.TimeMillisConversion());
        genericData.addLogicalTypeConversion(new TimeConversions.TimestampMicrosConversion());
        genericData.addLogicalTypeConversion(new TimeConversions.TimestampMillisConversion());

        GenericDatumWriter<GenericRecord> w = new GenericDatumWriter<>(schema, genericData);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(MAGIC_BYTE);
        outputStream.write(ByteBuffer.allocate(SCHEMA_ID_SIZE).putInt(schemaId).array());

        Encoder e = EncoderFactory.get().binaryEncoder(outputStream, null);

        w.write(genericRecord, e);
        e.flush();

        return outputStream.toByteArray();
    }
}
