package org.kafkahq.repositories;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.apache.avro.SchemaBuilder;
import org.junit.Before;
import org.junit.Test;
import org.kafkahq.BaseTest;
import org.kafkahq.KafkaTestCluster;
import org.kafkahq.models.Schema;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SchemaRegistryRepositoryTest extends BaseTest {
    private final SchemaRegistryRepository repository = app.require(SchemaRegistryRepository.class);

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
        .name("clientHash").type().fixed("MD5").size(16).noDefault()
        .name("clientProtocol").type().nullable().stringType().noDefault()
        .name("serverHash").type("MD5").noDefault()
        .name("meta").type().nullable().map().values().bytesType().noDefault()
        .endRecord();

    @Before
    public void cleanup() {
        try {
            repository.delete(KafkaTestCluster.CLUSTER_ID, SUBJECT_1);
            repository.delete(KafkaTestCluster.CLUSTER_ID, SUBJECT_2);
        } catch (Exception ignored) {
        }
    }

    @Test
    public void getAll() throws IOException, RestClientException {
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT_1, SCHEMA_1_V1);
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT_2, SCHEMA_2);
        List<Schema> all = repository.getAll(KafkaTestCluster.CLUSTER_ID);

        assertEquals(2, all.size());
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

        assertEquals(1, repository.getAll(KafkaTestCluster.CLUSTER_ID).size());
    }

    @Test
    public void delete() throws IOException, RestClientException {
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT_1, SCHEMA_1_V1);
        repository.delete(KafkaTestCluster.CLUSTER_ID, SUBJECT_1);

        assertEquals(0, repository.getAll(KafkaTestCluster.CLUSTER_ID).size());
    }

    @Test
    public void getDefaultConfig() throws IOException, RestClientException {
        assertEquals(Schema.Config.CompatibilityLevelConfig.BACKWARD, repository.getDefaultConfig(KafkaTestCluster.CLUSTER_ID).getCompatibilityLevel());
    }
}