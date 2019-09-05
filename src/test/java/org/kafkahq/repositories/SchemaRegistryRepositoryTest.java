package org.kafkahq.repositories;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.apache.avro.SchemaBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kafkahq.AbstractTest;
import org.kafkahq.KafkaTestCluster;
import org.kafkahq.models.Schema;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SchemaRegistryRepositoryTest extends AbstractTest {
    @Inject
    private SchemaRegistryRepository repository;

    private final String SUBJECT_1 = "SCHEMA_1";
    private final org.apache.avro.Schema SCHEMA_1_V1 = SchemaBuilder
        .record("schema1_v1").namespace("org.kafkahq")
        .fields()
        .name("clientHash").type().fixed("MD5").size(16).noDefault()
        .name("clientProtocol").type().nullable().stringType().noDefault()
        .name("serverHash").type("MD5").noDefault()
        .name("meta").type().nullable().map().values().bytesType().noDefault()
        .endRecord();

    private final org.apache.avro.Schema SCHEMA_1_V2 = SchemaBuilder
        .record("schema1_v2").namespace("org.kafkahq")
        .fields()
        .name("clientHash").type().fixed("MD5").size(16).noDefault()
        .name("clientProtocol").type().nullable().stringType().noDefault()
        .name("serverHash").type("MD5").noDefault()
        .name("meta").type().nullable().map().values().bytesType().noDefault()
        .name("serverIp").type().nullable().stringType().noDefault()
        .endRecord();

    private final String SUBJECT_2 = "SCHEMA_2";
    private final org.apache.avro.Schema SCHEMA_2 = SchemaBuilder
        .record("schema2").namespace("org.kafkahq")
        .fields()
        .name("hash").type().fixed("MD5").size(16).noDefault()
        .name("name").type().nullable().stringType().noDefault()
        .name("meta").type().nullable().map().values().stringType().noDefault()
        .endRecord();

    @BeforeEach
    public void cleanup() {
        try {
            repository.delete(KafkaTestCluster.CLUSTER_ID, SUBJECT_1);
            repository.delete(KafkaTestCluster.CLUSTER_ID, SUBJECT_2);
        } catch (Exception ignored) {
        }
    }

    @Test
    public void getAll() throws IOException, RestClientException {
        List<CompletableFuture<Schema>> all = repository.getAll(KafkaTestCluster.CLUSTER_ID, Optional.empty());
        assertEquals(3, all.size());
    }

    @Test
    public void getAllSearch() throws IOException, RestClientException {
        List<CompletableFuture<Schema>> all = repository.getAll(KafkaTestCluster.CLUSTER_ID, Optional.of("stream-count"));
        assertEquals(1, all.size());
    }

    @Test
    public void getStreamSubject() throws IOException, RestClientException {
        Schema latestVersion = repository.getLatestVersion(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_STREAM_MAP + "-value");

        assertNotNull(latestVersion);
    }

    @Test
    public void missingSubjectConfigMustBeDefault() throws IOException, RestClientException {
        Schema.Config defaultConfig = repository.getDefaultConfig(KafkaTestCluster.CLUSTER_ID);
        Schema.Config subjectConfig = repository.getConfig(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_STREAM_MAP);

        assertEquals(defaultConfig.getCompatibilityLevel(), subjectConfig.getCompatibilityLevel());
    }

    @Test
    public void getLatestVersion() throws IOException, RestClientException {
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT_1, SCHEMA_1_V1);
        repository.updateConfig(KafkaTestCluster.CLUSTER_ID, SUBJECT_1, new Schema.Config(Schema.Config.CompatibilityLevelConfig.NONE));
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT_1, SCHEMA_1_V2);
        Schema latestVersion = repository.getLatestVersion(KafkaTestCluster.CLUSTER_ID, SUBJECT_1);
        List<Schema> allVersions = repository.getAllVersions(KafkaTestCluster.CLUSTER_ID, SUBJECT_1);

        assertEquals(2, allVersions.size());
        assertEquals(SCHEMA_1_V2, latestVersion.getSchema());
    }

    @Test
    public void register() throws IOException, RestClientException {
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT_1, SCHEMA_1_V1);
        repository.updateConfig(KafkaTestCluster.CLUSTER_ID, SUBJECT_1, new Schema.Config(Schema.Config.CompatibilityLevelConfig.FORWARD));
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT_1, SCHEMA_1_V2);
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT_2, SCHEMA_2);

        assertEquals(5, repository.getAll(KafkaTestCluster.CLUSTER_ID, Optional.empty()).size());
        assertEquals(SCHEMA_1_V2, repository.getLatestVersion(KafkaTestCluster.CLUSTER_ID, SUBJECT_1).getSchema());
        assertEquals(2, repository.getAllVersions(KafkaTestCluster.CLUSTER_ID, SUBJECT_1).size());

        assertEquals(SCHEMA_2, repository.getLatestVersion(KafkaTestCluster.CLUSTER_ID, SUBJECT_2).getSchema());
    }

    @Test
    public void delete() throws IOException, RestClientException {
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT_1, SCHEMA_1_V1);
        repository.delete(KafkaTestCluster.CLUSTER_ID, SUBJECT_1);

        assertEquals(3, repository.getAll(KafkaTestCluster.CLUSTER_ID, Optional.empty()).size());
    }

    @Test
    public void getDefaultConfig() throws IOException, RestClientException {
        assertEquals(Schema.Config.CompatibilityLevelConfig.BACKWARD, repository.getDefaultConfig(KafkaTestCluster.CLUSTER_ID).getCompatibilityLevel());
    }
}