package org.akhq.modules.schemaregistry.apicurio;

import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.akhq.configs.Connection;
import org.akhq.models.Schema;
import org.akhq.repositories.SchemaRegistryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApicurioSchemaRegistryClientTest {
    private static final String AVRO_V1 = """
        {"type":"record","name":"Order","namespace":"com.acme","fields":[{"name":"id","type":"string"}]}""";
    private static final String AVRO_V2 = """
        {"type":"record","name":"Order","namespace":"com.acme","fields":[{"name":"id","type":"string"},{"name":"total","type":["null","double"],"default":null}]}""";
    private static final String AVRO_REF_CHILD = """
        {"type":"record","name":"Address","namespace":"com.acme","fields":[{"name":"city","type":"string"}]}""";
    private static final String AVRO_REF_PARENT = """
        {"type":"record","name":"Customer","namespace":"com.acme","fields":[{"name":"name","type":"string"},{"name":"address","type":"com.acme.Address"}]}""";
    private static final String JSON_SCHEMA = """
        {"$schema":"http://json-schema.org/draft-07/schema#","type":"object","properties":{"sku":{"type":"string"},"qty":{"type":"integer"}},"required":["sku"]}""";
    private static final String PROTO_SCHEMA = """
        syntax = "proto3";
        package com.acme;
        message Payment {
          string id = 1;
          int64 amount = 2;
        }
        """;

    private InMemoryApicurioV3Server server;

    @BeforeEach
    void setUp() throws Exception {
        server = new InMemoryApicurioV3Server();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    private ApicurioSchemaRegistryClient clientFor(String url, String user, String pass, String groupId, Map<String, String> props) {
        Connection.SchemaRegistry sr = ApicurioConfigTest.createSchemaRegistry(url, user, pass, groupId, props);
        return new ApicurioSchemaRegistryClient(new ApicurioConfig(sr));
    }

    @Test
    void listsMultipleGroupsFiltersNonSchemaArtifactsAndRespectsConfiguredGroup() throws Exception {
        server.seedArtifactVersion("default", "orders-value", "AVRO", AVRO_V1, "ENABLED", List.of());
        server.seedArtifactVersion("com.acme.billing", "payments-value", "PROTOBUF", PROTO_SCHEMA, "ENABLED", List.of());
        server.seedArtifactVersion("com.acme.catalog", "items-value", "JSON", JSON_SCHEMA, "ENABLED", List.of());
        server.seedArtifactVersion("default", "public-api", "OPENAPI", "{\"openapi\":\"3.0.3\"}", "ENABLED", List.of());

        // URL without /apis/registry/v3 is normalized automatically
        ApicurioSchemaRegistryClient allGroupsClient = clientFor(server.getBaseUrl(), null, null, null, null);
        List<String> allSubjects = allGroupsClient.listSubjects();
        assertEquals(3, allSubjects.size());
        assertTrue(allSubjects.contains("orders-value"));
        assertTrue(allSubjects.contains("com.acme.billing:payments-value"));
        assertTrue(allSubjects.contains("com.acme.catalog:items-value"));
        assertFalse(allSubjects.contains("public-api"));

        // Scoped to a specific group
        ApicurioSchemaRegistryClient scopedClient = clientFor(server.getV3Url(), null, null, "com.acme.billing", null);
        List<String> scopedSubjects = scopedClient.listSubjects();
        assertEquals(List.of("payments-value"), scopedSubjects);
    }

    @Test
    void crudVersionsAndDisabledVersionHandling() throws Exception {
        ApicurioSchemaRegistryClient client = clientFor(server.getV3Url(), null, null, null, null);

        // Register v1 in custom group
        Schema v1 = client.register("com.acme:orders-value", "AVRO", AVRO_V1, List.of());
        assertEquals("com.acme:orders-value", v1.getSubject());
        assertEquals(1, v1.getVersion());
        assertEquals("AVRO", v1.getSchemaType());
        assertNotNull(v1.getId());
        assertNotNull(v1.getAvroSchema());

        // Idempotent re-register returns existing version
        Schema v1Again = client.register("com.acme:orders-value", "AVRO", AVRO_V1, List.of());
        assertEquals(1, v1Again.getVersion());
        assertEquals(v1.getId(), v1Again.getId());

        // Register v2
        Schema v2 = client.register("com.acme:orders-value", "AVRO", AVRO_V2, List.of());
        assertEquals(2, v2.getVersion());
        assertNotEquals(v1.getId(), v2.getId());

        // Seed a disabled v3 — latest and getAllVersions must skip DISABLED!
        server.seedArtifactVersion("com.acme", "orders-value", "AVRO", AVRO_V1, "DISABLED", List.of());

        Schema latest = client.getLatestVersion("com.acme:orders-value");
        assertEquals(2, latest.getVersion());

        List<Schema> allVersions = client.getAllVersions("com.acme:orders-value");
        assertEquals(2, allVersions.size());
        assertEquals(1, allVersions.get(0).getVersion());
        assertEquals(2, allVersions.get(1).getVersion());

        // Delete v1
        assertEquals(1, client.deleteVersion("com.acme:orders-value", 1));
        assertEquals(1, client.getAllVersions("com.acme:orders-value").size());

        // Delete artifact
        client.delete("com.acme:orders-value");
        RestClientException notFound = assertThrows(
            RestClientException.class,
            () -> client.getLatestVersion("com.acme:orders-value")
        );
        assertEquals(404, notFound.getStatus());
        assertEquals(SchemaRegistryRepository.ERROR_NOT_FOUND, notFound.getErrorCode());
        // Artifact where all versions are DISABLED returns graceful exception metadata rather than failing list
        server.seedArtifactVersion("default", "all-disabled-value", "AVRO", AVRO_V1, "DISABLED", List.of());
        Schema disabledOnly = client.getLatestVersion("all-disabled-value");
        assertEquals("all-disabled-value", disabledOnly.getSubject());
        assertNull(disabledOnly.getSchema());
        assertNotNull(disabledOnly.getException());
        assertTrue(client.getAllVersions("all-disabled-value").isEmpty());
        client.delete("all-disabled-value");
    }

    @Test
    void registersAndParsesAvroJsonAndProtobufWithReferences() throws Exception {
        ApicurioSchemaRegistryClient client = clientFor(server.getV3Url(), null, null, null, null);

        // Avro with reference (using Confluent-style -1 version meaning latest)
        client.register("com.acme:address-value", "AVRO", AVRO_REF_CHILD, List.of());
        Schema customer = client.register(
            "com.acme:customer-value",
            "AVRO",
            AVRO_REF_PARENT,
            List.of(new SchemaReference("com.acme.Address", "com.acme:address-value", -1))
        );
        assertEquals("com.acme.Customer", customer.getAvroSchema().getFullName());
        assertEquals(1, customer.getReferences().size());
        assertEquals("com.acme:address-value", customer.getReferences().get(0).getSubject());

        Schema fetchedCustomer = client.getLatestVersion("com.acme:customer-value");
        assertEquals("com.acme.Customer", fetchedCustomer.getAvroSchema().getFullName());
        assertEquals(1, fetchedCustomer.getReferences().size());

        // JSON Schema
        Schema jsonSchema = client.register("items-value", "JSON", JSON_SCHEMA, List.of());
        assertEquals("JSON", jsonSchema.getSchemaType());
        assertNotNull(jsonSchema.getJsonSchema());

        // Protobuf
        Schema protoSchema = client.register("payments-value", "PROTOBUF", PROTO_SCHEMA, List.of());
        assertEquals("PROTOBUF", protoSchema.getSchemaType());
        assertTrue(protoSchema.getSchema().contains("message Payment"));
    }

    @Test
    void compatibilityRulesPrecedenceAndUpdate() throws Exception {
        server.seedArtifactVersion("com.acme", "orders-value", "AVRO", AVRO_V1, "ENABLED", List.of());
        ApicurioSchemaRegistryClient client = clientFor(server.getV3Url(), null, null, null, null);

        // Default when no rule configured -> NONE
        assertEquals(Schema.Config.CompatibilityLevelConfig.NONE, client.getDefaultConfig().getCompatibilityLevel());
        assertEquals(Schema.Config.CompatibilityLevelConfig.NONE, client.getConfig("com.acme:orders-value").getCompatibilityLevel());

        // Global rule applies when no group/artifact rule
        server.setGlobalCompatibilityRule("BACKWARD");
        assertEquals(Schema.Config.CompatibilityLevelConfig.BACKWARD, client.getDefaultConfig().getCompatibilityLevel());
        assertEquals(Schema.Config.CompatibilityLevelConfig.BACKWARD, client.getConfig("com.acme:orders-value").getCompatibilityLevel());

        // Group rule overrides global rule
        server.setGroupCompatibilityRule("com.acme", "FORWARD");
        assertEquals(Schema.Config.CompatibilityLevelConfig.FORWARD, client.getConfig("com.acme:orders-value").getCompatibilityLevel());

        // Artifact rule created (POST on 404) overrides group rule
        client.updateConfig("com.acme:orders-value", new Schema.Config(Schema.Config.CompatibilityLevelConfig.FULL));
        assertEquals(Schema.Config.CompatibilityLevelConfig.FULL, client.getConfig("com.acme:orders-value").getCompatibilityLevel());

        // Artifact rule updated (PUT)
        client.updateConfig("com.acme:orders-value", new Schema.Config(Schema.Config.CompatibilityLevelConfig.BACKWARD_TRANSITIVE));
        assertEquals(Schema.Config.CompatibilityLevelConfig.BACKWARD_TRANSITIVE, client.getConfig("com.acme:orders-value").getCompatibilityLevel());
    }

    @Test
    void testCompatibilityReturnsEmptyOnPassAndDetailedCausesOnViolation() throws Exception {
        server.seedArtifactVersion("default", "orders-value", "AVRO", AVRO_V1, "ENABLED", List.of());
        ApicurioSchemaRegistryClient client = clientFor(server.getV3Url(), null, null, null, null);
        org.apache.avro.Schema candidate = new org.apache.avro.Schema.Parser().parse(AVRO_V2);

        assertTrue(client.testCompatibility("orders-value", candidate).isEmpty());

        server.setRejectCompatibilityDryRun(true);
        List<String> violations = client.testCompatibility("orders-value", candidate);
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("Field removed without default"));
        assertTrue(violations.get(0).contains("/fields/email"));
    }

    @Test
    void getSubjectsBySchemaIdSupportsContentIdGlobalIdAndAutoFallback() throws Exception {
        InMemoryApicurioV3Server.StoredVersion s1 = server.seedArtifactVersion("default", "topic1-value", "AVRO", AVRO_V1, "ENABLED", List.of());
        InMemoryApicurioV3Server.StoredVersion s2 = server.seedArtifactVersion("com.acme", "topic2-value", "AVRO", AVRO_V1, "ENABLED", List.of());
        assertEquals(s1.contentId(), s2.contentId());
        assertNotEquals(s1.globalId(), s2.globalId());

        // Auto mode finds both subjects by shared contentId, AND falls back to globalId when contentId has no matches
        ApicurioSchemaRegistryClient autoClient = clientFor(server.getV3Url(), null, null, null, null);
        List<Schema> byContentId = autoClient.getSubjectsBySchemaId((int) s1.contentId());
        assertEquals(2, byContentId.size());
        assertTrue(byContentId.stream().anyMatch(s -> s.getSubject().equals("topic1-value")));
        assertTrue(byContentId.stream().anyMatch(s -> s.getSubject().equals("com.acme:topic2-value")));

        List<Schema> byGlobalIdFallback = autoClient.getSubjectsBySchemaId((int) s2.globalId());
        assertEquals(1, byGlobalIdFallback.size());
        assertEquals("com.acme:topic2-value", byGlobalIdFallback.get(0).getSubject());

        // Explicit globalId mode
        ApicurioSchemaRegistryClient gidClient = clientFor(
            server.getV3Url(), null, null, null, Map.of(ApicurioConfig.PROP_USE_ID, "globalId")
        );
        Schema latestGid = gidClient.getLatestVersion("com.acme:topic2-value");
        assertEquals((int) s2.globalId(), latestGid.getId());
        assertTrue(gidClient.getSubjectsBySchemaId(999999).isEmpty());
    }

    @Test
    void basicAndBearerAuthenticationHeadersAreSent() throws Exception {
        server.seedArtifactVersion("default", "secure-value", "AVRO", AVRO_V1, "ENABLED", List.of());

        String expectedBasic = "Basic " + Base64.getEncoder().encodeToString("admin:pass123".getBytes(StandardCharsets.UTF_8));
        server.setExpectedAuthorizationHeader(expectedBasic);

        ApicurioSchemaRegistryClient unauthClient = clientFor(server.getV3Url(), null, null, null, null);
        RestClientException unauthEx = assertThrows(RestClientException.class, unauthClient::listSubjects);
        assertEquals(401, unauthEx.getStatus());

        ApicurioSchemaRegistryClient basicClient = clientFor(server.getV3Url(), "admin", "pass123", null, null);
        assertEquals(List.of("secure-value"), basicClient.listSubjects());

        server.setExpectedAuthorizationHeader("Bearer my-jwt-token");
        ApicurioSchemaRegistryClient bearerClient = clientFor(
            server.getV3Url(), null, null, null, Map.of(ApicurioConfig.PROP_AUTH_BEARER_TOKEN, "my-jwt-token")
        );
        assertEquals(List.of("secure-value"), bearerClient.listSubjects());
    }
}
