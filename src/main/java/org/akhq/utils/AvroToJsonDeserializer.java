package org.akhq.utils;

import io.micronaut.core.serialize.exceptions.SerializationException;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.Connection;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;

import java.io.IOException;

/**
 * Class for deserialization of messages in Avro raw data binary format using topics mapping config.
 */
@Slf4j
public class AvroToJsonDeserializer {
    private final DecoderFactory decoderFactory = DecoderFactory.get();
    private final RawAvroUtils rawAvroUtils;
    private final AvroToJsonSerializer avroToJsonSerializer;

    public AvroToJsonDeserializer(Connection.Deserialization.AvroDeserializationTopicsMapping avroDeserializationTopicsMapping, AvroToJsonSerializer avroToJsonSerializer) {
        if (avroDeserializationTopicsMapping == null) {
            this.rawAvroUtils = null;
            this.avroToJsonSerializer = null;
        } else {
            this.rawAvroUtils = new RawAvroUtils(avroDeserializationTopicsMapping.getTopicsMapping(), avroDeserializationTopicsMapping.getSchemasFolder());
            this.avroToJsonSerializer = avroToJsonSerializer;
        }
    }

    /**
     * Deserialize from Avro raw data binary format to Json.
     * Messages must have been encoded directly with {@link org.apache.avro.io.DatumWriter}, not {@link org.apache.avro.file.DataFileWriter} or {@link org.apache.avro.message.BinaryMessageEncoder}.
     * Topic name should match topic-regex from {@code akhq.connections.[clusterName].deserialization.avro.topics-mapping} config
     * and schema should be set for key or value in that config.
     *
     * @param topic  current topic name
     * @param buffer binary data to decode
     * @param isKey  is this data represent key or value
     * @return {@code null} if cannot deserialize or configuration is not matching, return decoded string otherwise
     */
    public String deserialize(String topic, byte[] buffer, boolean isKey) {
        Schema schema = rawAvroUtils.getSchema(topic, isKey);

        if (schema == null) {
            return null;
        }

        String result;
        try {
            result = tryToDeserializeWithSchemaFile(buffer, schema);
        } catch (Exception e) {
            throw new SerializationException(String.format("Cannot deserialize message with Avro deserializer " +
                    "for topic [%s] and schema [%s]", topic, schema.getFullName()), e);
        }
        return result;
    }

    private String tryToDeserializeWithSchemaFile(byte[] buffer, Schema schema) throws IOException {
        DatumReader<?> reader = new GenericDatumReader<>(schema);
        Object result = reader.read(null, decoderFactory.binaryDecoder(buffer, null));

        //for primitive avro type
        if (!(result instanceof GenericRecord)) {
            return String.valueOf(result);
        }

        GenericRecord record = (GenericRecord) result;
        return avroToJsonSerializer.toJson(record);
    }
}
