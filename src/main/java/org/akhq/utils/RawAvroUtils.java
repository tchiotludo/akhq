package org.akhq.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.micronaut.core.serialize.exceptions.SerializationException;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.AvroTopicsMapping;
import org.apache.avro.Schema;

@Slf4j
public class RawAvroUtils {
    private final Map<String, Schema> keySchemas;
    private final Map<String, Schema> valueSchemas;
    private final List<AvroTopicsMapping> topicsMapping;
    private final String avroSchemasFolder;

    public RawAvroUtils(List<AvroTopicsMapping> topicsMapping, String avroSchemasFolder) {
        this.topicsMapping = topicsMapping;
        this.avroSchemasFolder = avroSchemasFolder;
        this.keySchemas = buildSchemas(AvroTopicsMapping::getKeySchemaFile);
        this.valueSchemas = buildSchemas(AvroTopicsMapping::getValueSchemaFile);
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

    private Schema loadSchemaFile(AvroTopicsMapping mapping, String schemaFile) throws IOException {
        if (avroSchemasFolder != null && Files.exists(Path.of(avroSchemasFolder))) {
            String fullPath = avroSchemasFolder + File.separator + schemaFile;
            return new Schema.Parser().parse(Path.of(fullPath).toFile());
        }
        throw new FileNotFoundException("Avro schema file is not found for topic regex [" +
            mapping.getTopicRegex() + "]. Folder is not specified or doesn't exist.");
    }

    Schema getSchema(String topic, boolean isKey) {
        AvroTopicsMapping matchingConfig = findMatchingConfig(topic);
        if (matchingConfig == null) {
            log.debug("Avro raw config is not found for topic [{}]", topic);
            return null;
        }

        if (matchingConfig.getKeySchemaFile() == null && matchingConfig.getValueSchemaFile() == null) {
            throw new SerializationException(String.format("Avro serialization is configured for topic [%s], " +
                "but schema is not specified neither for a key, nor for a value.", topic));
        }

        Schema schema;
        if (isKey) {
            return keySchemas.get(matchingConfig.getTopicRegex());
        } else {
            return valueSchemas.get(matchingConfig.getTopicRegex());
        }
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
}
