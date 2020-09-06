package org.akhq.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.annotations.VisibleForTesting;
import lombok.*;
import org.akhq.utils.AvroSchemaDeserializer;
import org.akhq.utils.AvroSchemaSerializer;
import org.apache.avro.AvroTypeException;
import org.apache.avro.Schema.Parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class Schema {
    @JsonIgnore
    private final Parser parser = new Parser();

    private Integer id;
    private String subject;
    private Integer version;
    private Config.CompatibilityLevelConfig compatibilityLevel;

    @JsonSerialize(using = AvroSchemaSerializer.class)
    @JsonDeserialize(using = AvroSchemaDeserializer.class)
    private org.apache.avro.Schema schema;

    private String exception;

    public Schema(Schema schema, Schema.Config config) {
        this.id = schema.id;
        this.subject = schema.subject;
        this.version = schema.version;
        this.schema = schema.getSchema();
        this.exception = schema.exception;
        this.compatibilityLevel = config.getCompatibilityLevel();
    }

    public Schema(io.confluent.kafka.schemaregistry.client.rest.entities.Schema schema, Schema.Config config) {
        this.id = schema.getId();
        this.subject = schema.getSubject();
        this.version = schema.getVersion();
        this.compatibilityLevel = config.getCompatibilityLevel();

        try {
            this.schema = parser.parse(schema.getSchema());
        } catch (AvroTypeException e) {
            this.schema = null;
            this.exception = e.getMessage();
        }
    }

    @VisibleForTesting
    public Schema(String subject, org.apache.avro.Schema schema, Config.CompatibilityLevelConfig compatibilityLevel) {
        this.subject = subject;
        this.schema = schema;
        this.compatibilityLevel = compatibilityLevel;
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @Builder
    @NoArgsConstructor
    public static class Config {
        public enum CompatibilityLevelConfig {
            NONE,
            BACKWARD,
            BACKWARD_TRANSITIVE,
            FORWARD,
            FORWARD_TRANSITIVE,
            FULL,
            FULL_TRANSITIVE
        }

        public static List<String> getCompatibilityLevelConfigList() {
            return new ArrayList<>(Arrays.asList(Schema.Config.CompatibilityLevelConfig
                .values()))
                .stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        }

        private CompatibilityLevelConfig compatibilityLevel;

        public Config(io.confluent.kafka.schemaregistry.client.rest.entities.Config config) {
            this.compatibilityLevel = CompatibilityLevelConfig.valueOf(config.getCompatibilityLevel());
        }

        public Config(CompatibilityLevelConfig config) {
            this.compatibilityLevel = config;
        }
    }
}
