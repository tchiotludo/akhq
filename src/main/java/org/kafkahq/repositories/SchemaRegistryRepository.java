package org.kafkahq.repositories;

import io.confluent.kafka.schemaregistry.client.rest.entities.requests.ConfigUpdateRequest;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.kafkahq.models.Schema;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.utils.PagedList;
import org.kafkahq.utils.Pagination;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class SchemaRegistryRepository extends AbstractRepository {
    public static final int ERROR_NOT_FOUND = 40401;

    @Inject
    private KafkaModule kafkaModule;
    private Map<String, KafkaAvroDeserializer> kafkaAvroDeserializers = new HashMap<>();

    public PagedList<Schema> list(String clusterId, Pagination pagination, Optional<String> search) throws IOException, RestClientException, ExecutionException, InterruptedException {
        return PagedList.of(all(clusterId, search), pagination, list -> list
            .stream()
            .map(s -> {
                try {
                    return getLatestVersion(clusterId, s);
                } catch (RestClientException | IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList())
        );
    }

    public List<String> all(String clusterId, Optional<String> search) throws  IOException, RestClientException {
        return kafkaModule
            .getRegistryRestClient(clusterId)
            .getAllSubjects()
            .stream()
            .filter(s -> isSearchMatch(search, s))
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

    public Schema getById(String clusterId, Integer id) throws IOException, RestClientException, ExecutionException, InterruptedException {
        for (String subject: this.all(clusterId, Optional.empty())) {
            for (Schema version: this.getAllVersions(clusterId, subject)) {
                if (version.getId().equals(id)) {
                    return version;
                }
            }
        }

        return null;
    }

    public Schema getLatestVersion(String clusterId, String subject) throws IOException, RestClientException {
        io.confluent.kafka.schemaregistry.client.rest.entities.Schema latestVersion = this.kafkaModule
            .getRegistryRestClient(clusterId)
            .getLatestVersion(subject);

        return new Schema(latestVersion);
    }

    public List<Schema> getAllVersions(String clusterId, String subject) throws IOException, RestClientException {
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
            .map(Schema::new)
            .collect(Collectors.toList());
    }

    public Schema lookUpSubjectVersion(String clusterId, String subject, org.apache.avro.Schema schema, boolean deleted) throws IOException, RestClientException {
        io.confluent.kafka.schemaregistry.client.rest.entities.Schema find = this.kafkaModule
            .getRegistryRestClient(clusterId)
            .lookUpSubjectVersion(schema.toString(), subject, deleted);

        return new Schema(find);
    }

    public boolean testCompatibility(String clusterId, String subject, org.apache.avro.Schema schema) throws IOException, RestClientException {
        return this.kafkaModule
            .getRegistryRestClient(clusterId)
            .testCompatibility(schema.toString(), subject, "latest");
    }

    public boolean testCompatibility(String clusterId, String subject, org.apache.avro.Schema schema, int version) throws IOException, RestClientException {
        return this.kafkaModule
            .getRegistryRestClient(clusterId)
            .testCompatibility(schema.toString(), subject, String.valueOf(version));
    }

    public Schema register(String clusterId, String subject, org.apache.avro.Schema schema) throws IOException, RestClientException {
        int id = this.kafkaModule
            .getRegistryRestClient(clusterId)
            .registerSchema(schema.toString(), subject);

        Schema latestVersion = getLatestVersion(clusterId, subject);

        if (latestVersion.getId() != id) {
            throw new IllegalArgumentException("Invalid id from registry expect " + id + " got last version " + latestVersion.getId());
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

        return list.get(0);
    }

    public int deleteVersion(String clusterId, String subject, int version) throws IOException, RestClientException {
        return this.kafkaModule.getRegistryRestClient(clusterId)
            .deleteSchemaVersion(new HashMap<>(), subject, String.valueOf(version));
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
                .getConfig(subject)
            );
        } catch (RestClientException exception) {
            if (exception.getErrorCode() != ERROR_NOT_FOUND) {
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
    }

    public KafkaAvroDeserializer getKafkaAvroDeserializer(String clusterId) {
        if (!this.kafkaAvroDeserializers.containsKey(clusterId)) {
            this.kafkaAvroDeserializers.put(
                clusterId,
                new KafkaAvroDeserializer(this.kafkaModule.getRegistryClient(clusterId))
            );
        }

        return this.kafkaAvroDeserializers.get(clusterId);
    }
}
