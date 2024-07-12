package org.akhq.utils;

import io.micronaut.core.serialize.exceptions.SerializationException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.AvroTopicsMapping;
import org.akhq.configs.Connection;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.*;

// TODO test + unify
/**
 * Class for serialization of messages in Json to Avro raw data binary format using topics mapping config.
 */
@Slf4j
public class JsonToAvroSerializer {
    private final EncoderFactory encoderFactory = EncoderFactory.get();
    private final Map<String, Schema> keySchemas;
    private final Map<String, Schema> valueSchemas;
    private final List<AvroTopicsMapping> topicsMapping;
    private final String avroSchemasFolder;

    public JsonToAvroSerializer(Connection.Serialization.AvroSerializationTopicsMapping avroSerializationTopicsMapping) {
        if (avroSerializationTopicsMapping == null) {
            this.keySchemas = new HashMap<>();
            this.valueSchemas = new HashMap<>();
            this.topicsMapping = new ArrayList<>();
            this.avroSchemasFolder = null;
        } else {
            this.avroSchemasFolder = avroSerializationTopicsMapping.getSchemasFolder();
            this.topicsMapping = avroSerializationTopicsMapping.getTopicsMapping();
            this.keySchemas = buildSchemas(AvroTopicsMapping::getKeySchemaFile);
            this.valueSchemas = buildSchemas(AvroTopicsMapping::getValueSchemaFile);
        }
    }

    /**
     * Load Avro schemas from schema folder
     *
     * @return map where keys are topic regexes and value is Avro schema
     */
    private Map<String, Schema> buildSchemas(Function<AvroTopicsMapping, String> schemaFileMapper) {
        Map<String, Schema> allSchemas = new HashMap<>();
        for (AvroTopicsMapping mapping : topicsMapping) {
            String schemaFile = schemaFileMapper.apply(mapping);

            if (schemaFile != null) {
                try {
                    Schema schema = loadSchemaFile(mapping, schemaFile);
                    allSchemas.put(mapping.getTopicRegex(), schema);
                } catch (IOException e) {
                    throw new RuntimeException(String.format("Cannot get a schema file for the topics regex [%s]", mapping.getTopicRegex()), e);
                }
            }
        }
        return allSchemas;
    }

    Schema loadSchemaFile(AvroTopicsMapping mapping, String schemaFile) throws IOException {
        if (avroSchemasFolder != null && Files.exists(Path.of(avroSchemasFolder))) {
            String fullPath = avroSchemasFolder + File.separator + schemaFile;
            return new Schema.Parser().parse(Path.of(fullPath).toFile());
        }
        throw new FileNotFoundException("Avro schema file is not found for topic regex [" +
            mapping.getTopicRegex() + "]. Folder is not specified or doesn't exist.");
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
        AvroTopicsMapping matchingConfig = findMatchingConfig(topic);
        if (matchingConfig == null) {
            log.debug("Avro serialization config is not found for topic [{}]", topic);
            return null;
        }

        if (matchingConfig.getKeySchemaFile() == null && matchingConfig.getValueSchemaFile() == null) {
            throw new SerializationException(String.format("Avro serialization is configured for topic [%s], " +
                "but schema is not specified neither for a key, nor for a value.", topic));
        }

        Schema schema;
        if (isKey) {
            schema = keySchemas.get(matchingConfig.getTopicRegex());
        } else {
            schema = valueSchemas.get(matchingConfig.getTopicRegex());
        }

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

    private AvroTopicsMapping findMatchingConfig(String topic) {
        for (AvroTopicsMapping mapping : topicsMapping) {
            if (topic.matches(mapping.getTopicRegex())) {
                return new AvroTopicsMapping(
                    mapping.getTopicRegex(),
                    mapping.getKeySchemaFile(), mapping.getValueSchemaFile());
            }
        }
        return null;
    }

    private byte[] tryToSerializeWithSchemaFile(String json, Schema schema) throws IOException {
        DatumReader<Object> reader = new GenericDatumReader<>(schema);
        GenericDatumWriter<Object> writer = new GenericDatumWriter<>(schema);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Decoder decoder = DecoderFactory.get().jsonDecoder(schema, json);
        Encoder encoder = EncoderFactory.get().binaryEncoder(output, null);
        Object datum = reader.read(null, decoder);
        writer.write(datum, encoder);
        encoder.flush();
        return output.toByteArray();
    }
}
