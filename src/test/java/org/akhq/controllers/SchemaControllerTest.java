package org.akhq.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.Schema;
import org.akhq.repositories.SchemaRegistryRepositoryTest;
import org.akhq.utils.ResultPagedList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchemaControllerTest extends AbstractTest {
    public static final String BASE_URL = "/api/" + KafkaTestCluster.CLUSTER_ID + "/schema";
    public static final String SCHEMA_URL = BASE_URL + "/" + KafkaTestCluster.TOPIC_STREAM_MAP + "-value";

    private static final Schema SCHEMA_1_V1 = new Schema(
        SchemaRegistryRepositoryTest.SUBJECT_1,
        SchemaRegistryRepositoryTest.SCHEMA_1_V1,
        Schema.Config.CompatibilityLevelConfig.FORWARD
    );

    private static final Schema SCHEMA_1_V2 = new Schema(
        SchemaRegistryRepositoryTest.SUBJECT_1,
        SchemaRegistryRepositoryTest.SCHEMA_1_V2,
        Schema.Config.CompatibilityLevelConfig.BACKWARD
    );

    @Test
    void listApi() {
        ResultPagedList<Schema> result = this.retrievePagedList(HttpRequest.GET(BASE_URL), Schema.class);
        assertEquals(3, result.getResults().size());
    }

    @Test
    void homeApi() {
        Schema result = this.retrieve(HttpRequest.GET(SCHEMA_URL), Schema.class);
        assertEquals("stream-map-value", result.getSubject());
    }

    @Test
    void crud() {
        Schema result;
        int version;

        // cleanup
        try {
            this.exchange(
                HttpRequest.DELETE(BASE_URL + "/" + SCHEMA_1_V1.getSubject())
            );
        } catch (Exception ignored) { }

        // create
        result = this.retrieve(HttpRequest.POST(BASE_URL, SCHEMA_1_V1), Schema.class);
        assertEquals(Schema.Config.CompatibilityLevelConfig.FORWARD, result.getCompatibilityLevel());
        version = result.getVersion();

        // duplicate
        HttpClientResponseException e = assertThrows(
            HttpClientResponseException.class,
            () -> {
                this.retrieve(HttpRequest.POST(BASE_URL, SCHEMA_1_V1), Schema.class);
            }
        );
        assertTrue(e.getMessage().contains("already exists"));

        // update
        result = this.retrieve(HttpRequest.POST(BASE_URL + "/" + SCHEMA_1_V1.getSubject(), SCHEMA_1_V2), Schema.class);
        assertEquals(SCHEMA_1_V2.getSchema(), result.getSchema());

        // delete version
        this.exchange(
            HttpRequest.DELETE(BASE_URL + "/" + SCHEMA_1_V1.getSubject() + "/version/" + version)
        );

        // version is deletd
        List<Schema> versions = this.retrieveList(HttpRequest.GET(SCHEMA_URL), Schema.class);
        assertEquals(1, versions.size());

        // delete
        this.exchange(
            HttpRequest.DELETE(BASE_URL + "/" + SCHEMA_1_V1.getSubject())
        );
    }

    @Test
    void versionsApi() {
        List<Schema> result = this.retrieveList(HttpRequest.GET(SCHEMA_URL + "/version"), Schema.class);
        assertEquals(1, result.size());
    }

    @Test
    void deleteNotExistApi() {
        HttpClientResponseException e = assertThrows(
            HttpClientResponseException.class,
            () -> {
                this.exchange(
                    HttpRequest.DELETE(BASE_URL + "/invalid")
                );
            }
        );
        assertTrue(e.getMessage().contains("doesn't exist"));
    }

    @Test
    void findSubjectBySchemaId() {
        // Create 3 subjects (including 2 with the same schema)
        var subject1Schema1 = new Schema("subjectTopic1-value", SchemaRegistryRepositoryTest.SCHEMA_1_V1,
            Schema.Config.CompatibilityLevelConfig.FORWARD);
        var subject1Schema1V2 = new Schema("subjectTopic1-value", SchemaRegistryRepositoryTest.SCHEMA_1_V2,
            Schema.Config.CompatibilityLevelConfig.FORWARD);
        var subject2Schema1 = new Schema("subjectTopic2-value", SchemaRegistryRepositoryTest.SCHEMA_1_V1,
            Schema.Config.CompatibilityLevelConfig.FORWARD);

        var subject1Schema1Response = this.retrieve(HttpRequest.POST(BASE_URL, subject1Schema1), Schema.class);
        var subject1Schema1V2Response = this.retrieve(HttpRequest.POST(BASE_URL + "/subjectTopic1-value", subject1Schema1V2), Schema.class);
        var subject2Schema1Response = this.retrieve(HttpRequest.POST(BASE_URL, subject2Schema1), Schema.class);

        // Subject v1 and v2 should be different
        assertNotEquals(subject1Schema1Response.getId(), subject1Schema1V2Response.getId());
        assertNotEquals(subject1Schema1Response.getSchema(), subject1Schema1V2Response.getSchema());

        // Subject 1 and 2 should have the same ID, schema but different subject
        assertEquals(subject1Schema1Response.getId(), subject2Schema1Response.getId());
        assertEquals(subject1Schema1Response.getSchema(), subject2Schema1Response.getSchema());
        assertNotEquals(subject1Schema1Response.getSubject(), subject2Schema1Response.getSubject());

        // Searching subject by schema ID should give the right subject depending on the topic
        var subject1FromSchemaIdAndTopic =
            this.retrieve(HttpRequest.GET(BASE_URL + "/id/" + subject1Schema1Response.getId() + "?topic=subjectTopic1"), Schema.class);
        assertEquals(subject1Schema1Response.getId(), subject1FromSchemaIdAndTopic.getId());
        assertEquals(subject1Schema1Response.getSubject(), subject1FromSchemaIdAndTopic.getSubject());

        var subject2FromSchemaIdAndTopic =
            this.retrieve(HttpRequest.GET(BASE_URL + "/id/" + subject1Schema1Response.getId() + "?topic=subjectTopic2"), Schema.class);
        assertEquals(subject2Schema1Response.getId(), subject2FromSchemaIdAndTopic.getId());
        assertEquals(subject2Schema1Response.getSubject(), subject2FromSchemaIdAndTopic.getSubject());

        // Clean
        this.exchange(HttpRequest.DELETE(BASE_URL + "/subjectTopic1-value"));
        this.exchange(HttpRequest.DELETE(BASE_URL + "/subjectTopic2-value"));
    }
}
