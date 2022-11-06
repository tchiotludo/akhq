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
        assertTrue(e.getMessage().contains("already exits"));

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
        assertTrue(e.getMessage().contains("doesn't exits"));
    }
}
