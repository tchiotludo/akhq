package org.akhq.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import lombok.*;
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
    private final Parser parser = new Parser().setValidateDefaults(false);

    private Integer id;
    private String subject;
    private Integer version;
    private Config.CompatibilityLevelConfig compatibilityLevel;
    private String schema;
    private String schemaType;
    private List<SchemaReference> references = new ArrayList<>();

    @JsonIgnore
    private org.apache.avro.Schema avroSchema;

    @JsonIgnore
    private JsonNode jsonSchema;

    private String exception;

    public Schema(Schema schema, Schema.Config config) {
        this.id = schema.id;
        this.subject = schema.subject;
        this.version = schema.version;
        this.schema = schema.getSchema();
        this.references = schema.getReferences();
        this.exception = schema.exception;
        this.compatibilityLevel = config.getCompatibilityLevel();
    }

    public Schema(io.confluent.kafka.schemaregistry.client.rest.entities.Schema schema, ParsedSchema parsedSchema, Schema.Config config) {
        this.id = schema.getId();
        this.subject = schema.getSubject();
        this.version = schema.getVersion();
        this.compatibilityLevel = config.getCompatibilityLevel();

        try {
            if (parsedSchema == null) {
                throw new AvroTypeException("Failed to parse schema " + schema.getSubject());
            }
            this.references = parsedSchema.references();
            this.schema = parsedSchema.rawSchema().toString();
            this.schemaType = schema.getSchemaType();
            if (schemaType.equals(AvroSchema.TYPE)) {
                this.avroSchema = parser.parse(this.schema);
            } else if ( schemaType.equals(JsonSchema.TYPE)) {
                this.jsonSchema = ((JsonSchema)parsedSchema).toJsonNode();
            } else if ( schemaType.equals(ProtobufSchema.TYPE)) {
                this.schema = parsedSchema.canonicalString();
            }
        } catch (AvroTypeException e) {
            this.schema = null;
            this.exception = e.getMessage();
        }
    }

    @VisibleForTesting
    public Schema(String subject, org.apache.avro.Schema schema, Config.CompatibilityLevelConfig compatibilityLevel) {
        this.subject = subject;
        this.avroSchema = schema;
        this.schema = this.avroSchema.toString();
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
