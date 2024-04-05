package org.akhq.repositories;

import com.amazonaws.services.schemaregistry.deserializers.GlueSchemaRegistryKafkaDeserializer;
import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants;
import com.amazonaws.services.schemaregistry.utils.AvroRecordType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;
import io.confluent.kafka.schemaregistry.client.rest.entities.requests.ConfigUpdateRequest;
import io.confluent.kafka.schemaregistry.client.rest.entities.requests.RegisterSchemaResponse;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.confluent.kafka.schemaregistry.utils.JacksonMapper;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.micronaut.context.ApplicationContext;
import jakarta.annotation.PostConstruct;
import org.akhq.configs.Connection;
import org.akhq.configs.SchemaRegistryType;
import org.akhq.models.Schema;
import org.akhq.models.audit.SchemaAuditEvent;
import org.akhq.modules.AuditModule;
import org.akhq.modules.KafkaModule;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;
import org.apache.kafka.common.serialization.Deserializer;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.kafka.common.serialization.StringDeserializer;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class SchemaRegistryRepository extends AbstractRepository {
    public static final int ERROR_NOT_FOUND = 40401;

    @Inject
    private KafkaModule kafkaModule;

    @Inject
    private ApplicationContext applicationContext;

    private AuditModule auditModule;

    private final Map<String, Deserializer> kafkaAvroDeserializers = new HashMap<>();
    private final Map<String, Deserializer> kafkaJsonDeserializers = new HashMap<>();
    private final Map<String, Deserializer> kafkaProtoDeserializers = new HashMap<>();
    private final Map<String, Deserializer> awsGlueKafkaDeserializers = new HashMap<>();

    @PostConstruct
    public void init() {
        if (applicationContext.containsBean(AuditModule.class)) {
            auditModule = applicationContext.getBean(AuditModule.class);
        }
    }

    public PagedList<Schema> list(String clusterId, Pagination pagination, Optional<String> search, List<String> filters) throws IOException, RestClientException, ExecutionException, InterruptedException {
        return PagedList.of(all(clusterId, search, filters), pagination, list -> this.toSchemasLatestVersion(list, clusterId));
    }

    public List<Schema> listAll(String clusterId, Optional<String> search, List<String> filters) throws IOException, RestClientException {
        return toSchemasLatestVersion(all(clusterId, search, filters), clusterId);
    }

    private List<Schema> toSchemasLatestVersion(List<String> subjectList, String clusterId){
        return subjectList .stream()
                .map(s -> {
                    try {
                        return getLatestVersion(clusterId, s);
                    } catch (RestClientException | IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    private ParsedSchema getParsedSchema(io.confluent.kafka.schemaregistry.client.rest.entities.Schema schema, String clusterId)  {
        ParsedSchema parsedSchema;
        if ( schema.getSchemaType().equals(JsonSchema.TYPE) ) {
            parsedSchema = this.kafkaModule
                .getJsonSchemaProvider(clusterId)
                .parseSchema(schema.getSchema(), schema.getReferences())
                .orElse(null);

        } else if( schema.getSchemaType().equals(ProtobufSchema.TYPE)) {
            parsedSchema = this.kafkaModule
                .getProtobufSchemaProvider(clusterId)
                .parseSchema(schema.getSchema(), schema.getReferences())
                .orElse(null);
        } else {
            parsedSchema = this.kafkaModule
                .getAvroSchemaProvider(clusterId)
                .parseSchema(schema.getSchema(), schema.getReferences())
                .orElse(null);
        }
        return parsedSchema;
    }

    public List<String> all(String clusterId, Optional<String> search, List<String> filters) throws  IOException, RestClientException {
        Optional<RestService> maybeRegistryRestClient = Optional.ofNullable(kafkaModule
                .getRegistryRestClient(clusterId));
        if(maybeRegistryRestClient.isEmpty()){
            return List.of();
        }
        return maybeRegistryRestClient.get()
            .getAllSubjects()
            .stream()
            .filter(s -> isSearchMatch(search, s) && isMatchRegex(filters, s))
            .sorted(Comparator.comparing(String::toLowerCase))
            .collect(Collectors.toList());
    }

    public boolean exist(String clusterId, String subject) throws IOException, RestClientException {
        boolean found = false;

        try {
            getLatestVersion(clusterId, subject);
            found = true;
        } catch (RestClientException exception) {
            if (exception.getErrorCode() != ERROR_NOT_FOUND) { // Subject not found.; error code: 40401
                throw exception;
            }
        }

        return found;
    }

    public List<Schema> getSubjectsBySchemaId(String clusterId, int id) throws  IOException, RestClientException {
        Optional<RestService> maybeRegistryRestClient = Optional.ofNullable(kafkaModule
            .getRegistryRestClient(clusterId));
        if (maybeRegistryRestClient.isEmpty()) {
            return List.of();
        }

        return maybeRegistryRestClient.get()
            .getAllVersionsById(id)
            .stream()
            .map(v -> new Schema(id, v.getSubject(), v.getVersion()))
            .collect(Collectors.toList());
    }

    public Optional<Schema> getById(String clusterId, Integer id) throws IOException, RestClientException, ExecutionException, InterruptedException {
        for (String subject: this.all(clusterId, Optional.empty(), List.of())) {
            for (Schema version: this.getAllVersions(clusterId, subject)) {
                if (version.getId().equals(id)) {
                    return Optional.of(version);
                }
            }
        }

        return Optional.empty();
    }

    public Schema getLatestVersion(String clusterId, String subject) throws IOException, RestClientException {
        io.confluent.kafka.schemaregistry.client.rest.entities.Schema latestVersion = this.kafkaModule
            .getRegistryRestClient(clusterId)
            .getLatestVersion(subject);

        ParsedSchema parsedSchema = getParsedSchema(latestVersion, clusterId);

        return new Schema(latestVersion, parsedSchema, this.getConfig(clusterId, subject));
    }

    public List<Schema> getAllVersions(String clusterId, String subject) throws IOException, RestClientException {
        Schema.Config config = this.getConfig(clusterId, subject);

        return this.kafkaModule
            .getRegistryRestClient(clusterId)
            .getAllVersions(subject)
            .parallelStream()
            .map(id -> {
                try {
                    return this.kafkaModule.getRegistryRestClient(clusterId).getVersion(subject, id);
                } catch (RestClientException | IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .map(schema -> {
                ParsedSchema parsedSchema = getParsedSchema(schema, clusterId);
                return new Schema(schema, parsedSchema, config);
            })
            .collect(Collectors.toList());
    }

    public Schema lookUpSubjectVersion(String clusterId, String subject, org.apache.avro.Schema schema, boolean deleted) throws IOException, RestClientException {
        io.confluent.kafka.schemaregistry.client.rest.entities.Schema find = this.kafkaModule
            .getRegistryRestClient(clusterId)
            .lookUpSubjectVersion(schema.toString(), subject, deleted);

        ParsedSchema parsedSchema = getParsedSchema(find, clusterId);

        return new Schema(find, parsedSchema, this.getConfig(clusterId, subject));
    }

    public List<String> testCompatibility(String clusterId, String subject, org.apache.avro.Schema schema) throws IOException, RestClientException {
        return this.kafkaModule
            .getRegistryRestClient(clusterId)
            .testCompatibility(schema.toString(), subject, "latest");
    }

    public List<String> testCompatibility(String clusterId, String subject, org.apache.avro.Schema schema, int version) throws IOException, RestClientException {
        return this.kafkaModule
            .getRegistryRestClient(clusterId)
            .testCompatibility(schema.toString(), subject, String.valueOf(version));
    }

    public Schema register(String clusterId, String subject, String schema, List<SchemaReference> references) throws IOException, RestClientException {
        return register(clusterId, subject, null, schema, references);
    }

    public Schema register(String clusterId, String subject, String type, String schema, List<SchemaReference> references) throws IOException, RestClientException {
        RegisterSchemaResponse registerSchemaResponse = this.kafkaModule
            .getRegistryRestClient(clusterId)
            .registerSchema(schema, type != null? type: "AVRO", references, subject);

        Schema latestVersion = getLatestVersion(clusterId, subject);

        if (latestVersion.getId() != registerSchemaResponse.getId()) {
            throw new IllegalArgumentException("Invalid id from registry expect " + registerSchemaResponse.getId() + " got last version " + latestVersion.getId());
        }

        if (auditModule != null) {
            auditModule.save(SchemaAuditEvent.createOrUpdateSchema(clusterId, subject, latestVersion.getId(), latestVersion.getVersion()));
        }

        return latestVersion;
    }

    public int delete(String clusterId, String subject) throws IOException, RestClientException {
        List<Integer> list = this.kafkaModule
            .getRegistryRestClient(clusterId)
            .deleteSubject(new HashMap<>(), subject);

        if (list.size() == 0) {
            throw new IllegalArgumentException("Invalid subject '" + subject + "'");
        }

        if (auditModule != null) {
            auditModule.save(SchemaAuditEvent.deleteSchema(clusterId, subject, list.get(0)));
        }

        return list.get(0);
    }

    public int deleteVersion(String clusterId, String subject, int version) throws IOException, RestClientException {
        var deletedVersion = this.kafkaModule.getRegistryRestClient(clusterId)
            .deleteSchemaVersion(new HashMap<>(), subject, String.valueOf(version));

        if (auditModule != null) {
            auditModule.save(SchemaAuditEvent.deleteSchema(clusterId, subject, deletedVersion));
        }

        return deletedVersion;
    }

    public Schema.Config getDefaultConfig(String clusterId) throws IOException, RestClientException {
        return new Schema.Config(this.kafkaModule
            .getRegistryRestClient(clusterId)
            .getConfig(null)
        );
    }

    public Schema.Config getConfig(String clusterId, String subject) throws IOException, RestClientException {
        try {
            return new Schema.Config(this.kafkaModule
                .getRegistryRestClient(clusterId)
                .getConfig(Map.of(), subject, true)
            );
        } catch (RestClientException exception) {
            if (exception.getStatus() != 404) {
                throw exception;
            }

            return this.getDefaultConfig(clusterId);
        }
    }

    public void updateConfig(String clusterId, String subject, Schema.Config config) throws IOException, RestClientException {
        ConfigUpdateRequest configUpdateRequest = this.kafkaModule
            .getRegistryRestClient(clusterId)
            .updateCompatibility(config.getCompatibilityLevel().name(), subject);

        if (!configUpdateRequest.getCompatibilityLevel().equals(config.getCompatibilityLevel().name())) {
            throw new IllegalArgumentException("Invalid config for '" + subject + "' current: '" + configUpdateRequest.getCompatibilityLevel() + "' expected: " + config.getCompatibilityLevel().name());
        }

        if (auditModule != null) {
            auditModule.save(SchemaAuditEvent.updateSchemaCompatibility(clusterId, subject, null, config.getCompatibilityLevel().name()));
        }
    }

    public Deserializer getKafkaAvroDeserializer(String clusterId) {
        if (!this.kafkaAvroDeserializers.containsKey(clusterId)) {
            Deserializer deserializer;
            SchemaRegistryType schemaRegistryType = getSchemaRegistryType(clusterId);
            if (schemaRegistryType == SchemaRegistryType.TIBCO) {
                try {
                    deserializer = (Deserializer) Class.forName("com.tibco.messaging.kafka.avro.AvroDeserializer").getDeclaredConstructor().newInstance();
                    Map<String, String> config = new HashMap<>();
                    config.put("schema.registry.url", this.kafkaModule.getConnection(clusterId).getSchemaRegistry().getUrl());
                    if (this.kafkaModule.getConnection(clusterId).getSchemaRegistry().getBasicAuthUsername() != null) {
                        config.put("ftl.username", this.kafkaModule.getConnection(clusterId).getSchemaRegistry().getBasicAuthUsername());
                        config.put("ftl.password", this.kafkaModule.getConnection(clusterId).getSchemaRegistry().getBasicAuthPassword());
                    }
                    config.putAll(this.kafkaModule.getConnection(clusterId).getProperties());
                    deserializer.configure(config, false);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
                    throw new IllegalArgumentException("Configured schema registry type was 'tibco', but TIBCO Avro client library not found on classpath");
                }
            } else {
                deserializer = new KafkaAvroDeserializer(this.kafkaModule.getRegistryClient(clusterId));
            }

            this.kafkaAvroDeserializers.put(clusterId, deserializer);
        }

        return this.kafkaAvroDeserializers.get(clusterId);
    }

    public Deserializer getKafkaJsonDeserializer(String clusterId) {
        if (!this.kafkaJsonDeserializers.containsKey(clusterId)) {
            Deserializer deserializer;
            SchemaRegistryType schemaRegistryType = getSchemaRegistryType(clusterId);
            if (schemaRegistryType == SchemaRegistryType.TIBCO) {
                throw new IllegalArgumentException("Configured schema registry type was 'tibco', but TIBCO JSON client is not supported");
            } else {
                deserializer = new KafkaJsonSchemaDeserializer(this.kafkaModule.getRegistryClient(clusterId));
            }

            this.kafkaJsonDeserializers.put(clusterId, deserializer);
        }

        return this.kafkaJsonDeserializers.get(clusterId);
    }

    public Deserializer getKafkaProtoDeserializer(String clusterId) {
        if (!this.kafkaProtoDeserializers.containsKey(clusterId)) {
            Deserializer deserializer;
            SchemaRegistryType schemaRegistryType = getSchemaRegistryType(clusterId);
            if (schemaRegistryType == SchemaRegistryType.TIBCO) {
                throw new IllegalArgumentException("Configured schema registry type was 'tibco', but TIBCO PROTOBUF client is not supported");
            } else {
                deserializer = new KafkaProtobufDeserializer(this.kafkaModule.getRegistryClient(clusterId));
            }

            this.kafkaProtoDeserializers.put(clusterId, deserializer);
        }

        return this.kafkaProtoDeserializers.get(clusterId);
    }

    public SchemaRegistryType getSchemaRegistryType(String clusterId) {
        SchemaRegistryType schemaRegistryType = SchemaRegistryType.CONFLUENT;
        Connection.SchemaRegistry schemaRegistry = this.kafkaModule.getConnection(clusterId).getSchemaRegistry();
        if (schemaRegistry != null) {
            schemaRegistryType = schemaRegistry.getType();
        }
        return schemaRegistryType;
    }
    public Deserializer getAwsGlueKafkaDeserializer(String clusterId) {

        if (!this.awsGlueKafkaDeserializers.containsKey(clusterId)){
            Connection.SchemaRegistry schemaRegistry = kafkaModule.getConnection(clusterId).getSchemaRegistry();
            Map<String, Object> params = new HashMap<>();
            params.put(AWSSchemaRegistryConstants.REGISTRY_NAME, schemaRegistry.getGlueSchemaRegistryName());
            params.put(AWSSchemaRegistryConstants.AWS_REGION,schemaRegistry.getAwsRegion());
            params.put(AWSSchemaRegistryConstants.AVRO_RECORD_TYPE, AvroRecordType.GENERIC_RECORD.getName());

            // Adding secondary deserializer so that messages that aren't serialized using avro,proto or json are deserialized using StringDeserializer
            params.put(AWSSchemaRegistryConstants.SECONDARY_DESERIALIZER, StringDeserializer.class.getName());
            Map<String, String> otherParams = schemaRegistry.getProperties();
            if (otherParams != null) {
                params.putAll(otherParams);
            }
            this.awsGlueKafkaDeserializers.put(clusterId, new GlueSchemaRegistryKafkaDeserializer(DefaultCredentialsProvider.builder().build(), params));
        }
        return this.awsGlueKafkaDeserializers.get(clusterId);
    }

    static {
        JacksonMapper.INSTANCE.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
