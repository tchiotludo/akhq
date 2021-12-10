package org.akhq.utils;

import io.micronaut.core.serialize.exceptions.SerializationException;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.AvroTopicsMapping;
import org.akhq.configs.Connection;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;

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

/**
 * Class for deserialization of messages in Avro raw data binary format using topics mapping config.
 */
@Slf4j
public class AvroToJsonDeserializer {
    private final DecoderFactory decoderFactory = DecoderFactory.get();
    private final Map<String, Schema> keySchemas;
    private final Map<String, Schema> valueSchemas;
    private final List<AvroTopicsMapping> topicsMapping;
    private final String avroSchemasFolder;
    private final AvroToJsonSerializer avroToJsonSerializer;

    public AvroToJsonDeserializer(Connection.Deserialization.AvroDeserializationTopicsMapping avroDeserializationTopicsMapping, AvroToJsonSerializer avroToJsonSerializer) {
        if (avroDeserializationTopicsMapping == null) {
            this.keySchemas = new HashMap<>();
            this.valueSchemas = new HashMap<>();
            this.topicsMapping = new ArrayList<>();
            this.avroSchemasFolder = null;
            this.avroToJsonSerializer = null;
        } else {
            this.avroSchemasFolder = avroDeserializationTopicsMapping.getSchemasFolder();
            this.topicsMapping = avroDeserializationTopicsMapping.getTopicsMapping();
            this.keySchemas = buildSchemas(AvroTopicsMapping::getKeySchemaFile);
            this.valueSchemas = buildSchemas(AvroTopicsMapping::getValueSchemaFile);
            this.avroToJsonSerializer = avroToJsonSerializer;
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
        AvroTopicsMapping matchingConfig = findMatchingConfig(topic);
        if (matchingConfig == null) {
            log.debug("Avro deserialization config is not found for topic [{}]", topic);
            return null;
        }

        if (matchingConfig.getKeySchemaFile() == null && matchingConfig.getValueSchemaFile() == null) {
            throw new SerializationException(String.format("Avro deserialization is configured for topic [%s], " +
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

        String result;
        try {
            result = tryToDeserializeWithSchemaFile(buffer, schema);
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
