package org.akhq.repositories;

import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.apache.avro.SchemaBuilder;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.Schema;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaRegistryRepositoryTest extends AbstractTest {
    @Inject
    private SchemaRegistryRepository repository;

    public final static String SUBJECT_1 = "SCHEMA_1";
    public final static org.apache.avro.Schema SCHEMA_1_V1 = SchemaBuilder
        .record("schema1").namespace("org.akhq")
        .fields()
        .name("clientHash").type().fixed("MD5").size(16).noDefault()
        .name("clientProtocol").type().nullable().stringType().noDefault()
        .name("serverHash").type("MD5").noDefault()
        .name("meta").type().nullable().map().values().bytesType().noDefault()
        .endRecord();

    public final static org.apache.avro.Schema SCHEMA_1_V2 = SchemaBuilder
        .record("schema1").namespace("org.akhq")
        .fields()
        .name("clientHash").type().fixed("MD5").size(16).noDefault()
        .name("clientProtocol").type().nullable().stringType().noDefault()
        .name("serverHash").type("MD5").noDefault()
        .name("meta").type().nullable().map().values().bytesType().noDefault()
        .name("serverIp").type().nullable().stringType().noDefault()
        .endRecord();

    public final static String SUBJECT_2 = "SCHEMA_2";
    public final static org.apache.avro.Schema SCHEMA_2 = SchemaBuilder
        .record("schema2").namespace("org.akhq")
        .fields()
        .name("hash").type().fixed("MD5").size(16).noDefault()
        .name("name").type().nullable().stringType().noDefault()
        .name("meta").type().nullable().map().values().stringType().noDefault()
        .endRecord();

    public final static String SUBJECT_3 = "SCHEMA_3";
    public final static String SCHEMA_3 = "{\"type\":\"record\",\"name\":\"Schema3\",\"namespace\":\"org.akhq\",\"fields\":[{\"name\":\"name\",\"type\":[\"null\",\"string\"]}]}";

    public final static String SUBJECT_4 = "SCHEMA_4";
    public final static String SCHEMA_4 = "{\"name\":\"Schema4\",\"namespace\":\"org.akhq\",\"type\":\"record\",\"fields\":[{\"name\":\"name\",\"type\":[\"null\",\"string\"]},{\"name\":\"schema3\",\"type\":\"Schema3\"}]}";

    @BeforeEach
    public void cleanup() {
        try {
            repository.delete(KafkaTestCluster.CLUSTER_ID, SUBJECT_1);
            repository.delete(KafkaTestCluster.CLUSTER_ID, SUBJECT_2);
            repository.delete(KafkaTestCluster.CLUSTER_ID, SUBJECT_4);
            repository.delete(KafkaTestCluster.CLUSTER_ID, SUBJECT_3);
        } catch (Exception ignored) {
        }
    }

    @Test
    public void getAll() throws IOException, RestClientException, ExecutionException, InterruptedException {
        PagedList<Schema> all = repository.list(
            KafkaTestCluster.CLUSTER_ID,
            new Pagination(100, URIBuilder.empty(), 1),
            Optional.empty()
        );
        assertEquals(3, all.size());
    }

    @Test
    public void getAllSearch() throws IOException, RestClientException, ExecutionException, InterruptedException {
        PagedList<Schema> all = repository.list(
            KafkaTestCluster.CLUSTER_ID,
            new Pagination(100, URIBuilder.empty(), 1),
            Optional.of("stream-count")
        );
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
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT_1, SCHEMA_1_V1.toString(), Collections.emptyList());
        repository.updateConfig(KafkaTestCluster.CLUSTER_ID, SUBJECT_1, new Schema.Config(Schema.Config.CompatibilityLevelConfig.NONE));
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT_1, SCHEMA_1_V2.toString(), Collections.emptyList());
        Schema latestVersion = repository.getLatestVersion(KafkaTestCluster.CLUSTER_ID, SUBJECT_1);
        List<Schema> allVersions = repository.getAllVersions(KafkaTestCluster.CLUSTER_ID, SUBJECT_1);

        assertEquals(2, allVersions.size());
        assertEquals(SCHEMA_1_V2, latestVersion.getAvroSchema());
    }

    @Test
    public void getLatestVersionWithReferences() throws IOException, RestClientException {
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT_3, SCHEMA_3, Collections.emptyList());
        repository.updateConfig(KafkaTestCluster.CLUSTER_ID, SUBJECT_3, new Schema.Config(Schema.Config.CompatibilityLevelConfig.NONE));
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT_4, SCHEMA_4, Collections.singletonList(new SchemaReference("Schema3", SUBJECT_3, -1)));
        repository.updateConfig(KafkaTestCluster.CLUSTER_ID, SUBJECT_4, new Schema.Config(Schema.Config.CompatibilityLevelConfig.NONE));

        Schema latestVersion = repository.getLatestVersion(KafkaTestCluster.CLUSTER_ID, SUBJECT_4);

        assertEquals("org.akhq.Schema4", latestVersion.getAvroSchema().getFullName());
        assertTrue(latestVersion.getAvroSchema().getFields().stream().anyMatch(field -> field.name().equals("schema3")));
        assertEquals(1, latestVersion.getReferences().size());
        assertEquals("Schema3", latestVersion.getReferences().get(0).getName());
        assertEquals(SUBJECT_3, latestVersion.getReferences().get(0).getSubject());
    }

    @Test
    public void register() throws IOException, RestClientException, ExecutionException, InterruptedException {
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT_1, SCHEMA_1_V1.toString(), Collections.emptyList());
        repository.updateConfig(KafkaTestCluster.CLUSTER_ID, SUBJECT_1, new Schema.Config(Schema.Config.CompatibilityLevelConfig.FORWARD));
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT_1, SCHEMA_1_V2.toString(), Collections.emptyList());
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT_2, SCHEMA_2.toString(), Collections.emptyList());

        assertEquals(5, repository.list(KafkaTestCluster.CLUSTER_ID, new Pagination(100, URIBuilder.empty(), 1), Optional.empty()).size());
        assertEquals(SCHEMA_1_V2, repository.getLatestVersion(KafkaTestCluster.CLUSTER_ID, SUBJECT_1).getAvroSchema());
        assertEquals(2, repository.getAllVersions(KafkaTestCluster.CLUSTER_ID, SUBJECT_1).size());

        assertEquals(SCHEMA_2, repository.getLatestVersion(KafkaTestCluster.CLUSTER_ID, SUBJECT_2).getAvroSchema());
    }

    @Test
    public void delete() throws IOException, RestClientException, ExecutionException, InterruptedException {
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT_1, SCHEMA_1_V1.toString(), Collections.emptyList());
        repository.delete(KafkaTestCluster.CLUSTER_ID, SUBJECT_1);

        assertEquals(3, repository.list(KafkaTestCluster.CLUSTER_ID, new Pagination(100, URIBuilder.empty(), 1), Optional.empty()).size());
    }

    @Test
    public void getDefaultConfig() throws IOException, RestClientException {
        assertEquals(Schema.Config.CompatibilityLevelConfig.BACKWARD, repository.getDefaultConfig(KafkaTestCluster.CLUSTER_ID).getCompatibilityLevel());
    }
}
