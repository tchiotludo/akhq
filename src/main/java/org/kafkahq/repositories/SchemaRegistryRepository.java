package org.kafkahq.repositories;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import io.confluent.kafka.schemaregistry.client.rest.entities.requests.ConfigUpdateRequest;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.jooby.Env;
import org.jooby.Jooby;
import org.kafkahq.models.Schema;
import org.kafkahq.modules.KafkaModule;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class SchemaRegistryRepository extends AbstractRepository implements Jooby.Module {
    private KafkaModule kafkaModule;

    @Inject
    public SchemaRegistryRepository(KafkaModule kafkaModule) {
        this.kafkaModule = kafkaModule;
    }

    public List<Schema> getAll(String clusterId) throws IOException, RestClientException {
        return this.kafkaModule
            .getRegistryClient(clusterId)
            .getAllSubjects()
            .stream()
            .map(s -> {
                try {
                    return getLatestVersion(clusterId, s);
                } catch (RestClientException | IOException e) {
                    throw new RuntimeException(e);
                }

            })
            .collect(Collectors.toList());
    }

    public boolean exist(String clusterId, String subject) throws IOException, RestClientException {
        boolean found = false;

        try {
            getLatestVersion(clusterId, subject);
            found = true;
        } catch (RestClientException exception) {
            if (exception.getErrorCode() != 40401) { // Subject not found.; error code: 40401
                throw exception;
            }
        }

        return found;
    }

    public Schema getLatestVersion(String clusterId, String subject) throws IOException, RestClientException {
        io.confluent.kafka.schemaregistry.client.rest.entities.Schema latestVersion = this.kafkaModule
            .getRegistryClient(clusterId)
            .getLatestVersion(subject);

        return new Schema(latestVersion);
    }

    public List<Schema> getAllVersions(String clusterId, String subject) throws IOException, RestClientException {
        return this.kafkaModule
            .getRegistryClient(clusterId)
            .getAllVersions(subject)
            .parallelStream()
            .map(id -> {
                try {
                    return this.kafkaModule.getRegistryClient(clusterId).getVersion(subject, id);
                } catch (RestClientException | IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .map(Schema::new)
            .collect(Collectors.toList());
    }

    public Schema lookUpSubjectVersion(String clusterId, String subject, org.apache.avro.Schema schema, boolean deleted) throws IOException, RestClientException {
        io.confluent.kafka.schemaregistry.client.rest.entities.Schema find = this.kafkaModule
            .getRegistryClient(clusterId)
            .lookUpSubjectVersion(schema.toString(), subject, deleted);

        return new Schema(find);
    }

    public boolean testCompatibility(String clusterId, String subject, org.apache.avro.Schema schema) throws IOException, RestClientException {
        return this.kafkaModule
            .getRegistryClient(clusterId)
            .testCompatibility(schema.toString(), subject, "latest");
    }

    public boolean testCompatibility(String clusterId, String subject, org.apache.avro.Schema schema, int version) throws IOException, RestClientException {
        return this.kafkaModule
            .getRegistryClient(clusterId)
            .testCompatibility(schema.toString(), subject, String.valueOf(version));
    }

    public Schema register(String clusterId, String subject, org.apache.avro.Schema schema) throws IOException, RestClientException {
        int id = this.kafkaModule
            .getRegistryClient(clusterId)
            .registerSchema(schema.toString(), subject);

        Schema latestVersion = getLatestVersion(clusterId, subject);

        if (latestVersion.getId() != id) {
            throw new IllegalArgumentException("Invalid id from registry expect " + id + " got last version " + latestVersion.getId());
        }

        return latestVersion;
    }

    public int delete(String clusterId, String subject) throws IOException, RestClientException {
        List<Integer> list = this.kafkaModule
            .getRegistryClient(clusterId)
            .deleteSubject(new HashMap<>(), subject);

        if (list.size() == 0) {
            throw new IllegalArgumentException("Invalid subject '" + subject + "'");
        }

        return list.get(0);
    }

    public int deleteVersion(String clusterId, String subject, int version) throws IOException, RestClientException {
        return this.kafkaModule.getRegistryClient(clusterId)
            .deleteSchemaVersion(new HashMap<>(), subject, String.valueOf(version));
    }

    public Schema.Config getDefaultConfig(String clusterId) throws IOException, RestClientException {
        return new Schema.Config(this.kafkaModule
            .getRegistryClient(clusterId)
            .getConfig(null)
        );
    }

    public Schema.Config getConfig(String clusterId, String subject) throws IOException, RestClientException {
        return new Schema.Config(this.kafkaModule
            .getRegistryClient(clusterId)
            .getConfig(subject)
        );
    }

    public void updateConfig(String clusterId, String subject, Schema.Config config) throws IOException, RestClientException {
        ConfigUpdateRequest configUpdateRequest = this.kafkaModule
            .getRegistryClient(clusterId)
            .updateCompatibility(config.getCompatibilityLevel().name(), subject);

        if (!configUpdateRequest.getCompatibilityLevel().equals(config.getCompatibilityLevel().name())) {
            throw new IllegalArgumentException("Invalid config for '" + subject + "' current: '" + configUpdateRequest.getCompatibilityLevel() + "' expected: " + config.getCompatibilityLevel().name());
        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(SchemaRegistryRepository.class);
    }
}
