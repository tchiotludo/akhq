package org.akhq.utils;

import io.micronaut.core.serialize.exceptions.SerializationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.Connection;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;

// TODO test
/**
 * Class for serialization of messages in Json to Avro raw data binary format using topics mapping config.
 */
@Slf4j
public class JsonToAvroSerializer {
    private final EncoderFactory encoderFactory = EncoderFactory.get();
    private final DecoderFactory decoderFactory = DecoderFactory.get();
    private final RawAvroUtils rawAvroUtils;

    public JsonToAvroSerializer(Connection.Serialization.AvroSerializationTopicsMapping avroSerializationTopicsMapping) {
        if (avroSerializationTopicsMapping == null) {
            this.rawAvroUtils = null;
        } else {
            this.rawAvroUtils = new RawAvroUtils(avroSerializationTopicsMapping.getTopicsMapping(), avroSerializationTopicsMapping.getSchemasFolder());
        }
    }

    /**
     * Serialize from Json to Avro raw data binary format.
     * Messages must be decoded directly with {@link org.apache.avro.io.DatumReader}, not {@link org.apache.avro.file.DataFileReader} or {@link org.apache.avro.message.BinaryMessageDecoder}.
     * Topic name should match topic-regex from {@code akhq.connections.[clusterName].serialization.avro.topics-mapping} config
     * and schema should be set for key or value in that config.
     *
     * @param topic  current topic name
     * @param data   Json data to encode
     * @param isKey  is this data represent key or value
     * @return {@code null} if cannot serialize or configuration is not matching, return encoded string otherwise
     */
    public byte[] serialize(String topic, String data, boolean isKey) {
        Schema schema = rawAvroUtils.getSchema(topic, isKey);

        if (schema == null) {
            return null;
        }

        byte[] result;
        try {
            result = tryToSerializeWithSchemaFile(data, schema);
        } catch (Exception e) {
            throw new SerializationException(String.format("Cannot deserialize message with Avro deserializer " +
                "for topic [%s] and schema [%s]", topic, schema.getFullName()), e);
        }
        return result;
    }

    private byte[] tryToSerializeWithSchemaFile(String json, Schema schema) throws IOException {
        DatumReader<Object> reader = new GenericDatumReader<>(schema);
        GenericDatumWriter<Object> writer = new GenericDatumWriter<>(schema);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Decoder decoder = decoderFactory.jsonDecoder(schema, json);
        Encoder encoder = encoderFactory.binaryEncoder(output, null);
        Object datum = reader.read(null, decoder);
        writer.write(datum, encoder);
        encoder.flush();
        return output.toByteArray();
    }
}
