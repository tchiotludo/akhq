package org.akhq.modules.schemaregistry.apicurio;

import org.akhq.configs.Connection;
import org.akhq.configs.SchemaRegistryType;
import org.akhq.mcp.model.FindSchemasForTopicResult;
import org.akhq.mcp.model.GetSchemaByIdResult;
import org.akhq.mcp.model.GetSchemaResult;
import org.akhq.mcp.model.SearchSchemasResult;
import org.akhq.mcp.services.AkhqSchemaToolService;
import org.akhq.middlewares.SchemaComparator;
import org.akhq.models.Schema;
import org.akhq.models.audit.SchemaAuditEvent;
import org.akhq.modules.AuditModule;
import org.akhq.modules.KafkaModule;
import org.akhq.modules.schemaregistry.RecordWithSchemaSerializerFactory;
import org.akhq.modules.schemaregistry.SchemaSerializer;
import org.akhq.repositories.SchemaRegistryRepository;
import org.akhq.utils.AvroToJsonSerializer;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ApicurioIntegrationTest {
    private static final String CLUSTER_ID = "apicurio-cluster";
    private static final String ORDER_AVRO_V1 = """
        {"type":"record","name":"Order","namespace":"com.acme","fields":[{"name":"id","type":"string"}]}""";
    private static final String ORDER_AVRO_V2 = """
        {"type":"record","name":"Order","namespace":"com.acme","fields":[{"name":"id","type":"string"},{"name":"note","type":["null","string"],"default":null}]}""";

    private InMemoryApicurioV3Server server;
    private KafkaModule kafkaModule;
    private AuditModule auditModule;
    private SchemaRegistryRepository repository;
    private RecordWithSchemaSerializerFactory serializerFactory;

    @BeforeEach
    void setUp() throws Exception {
        server = new InMemoryApicurioV3Server();

        Connection connection = new Connection(CLUSTER_ID);
        Connection.SchemaRegistry sr = ApicurioConfigTest.createSchemaRegistry(server.getV3Url(), null, null, null, null);
        setField(connection, "schemaRegistry", sr);

        kafkaModule = new KafkaModule();
        setField(kafkaModule, "connections", List.of(connection));
        setField(kafkaModule, "defaults", List.of());

        auditModule = mock(AuditModule.class);

        repository = new SchemaRegistryRepository();
        setField(repository, "kafkaModule", kafkaModule);
        setField(repository, "auditModule", auditModule);

        serializerFactory = new RecordWithSchemaSerializerFactory(kafkaModule);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void repositoryFullLifecycleAuditAndSerializerFactory() throws Exception {
        assertEquals(SchemaRegistryType.APICURIO, repository.getSchemaRegistryType(CLUSTER_ID));
        assertNull(kafkaModule.getRegistryRestClient(CLUSTER_ID));
        assertNull(kafkaModule.getRegistryClient(CLUSTER_ID));
        assertNotNull(kafkaModule.getApicurioSchemaRegistryClient(CLUSTER_ID));

        // Register across default and custom groups
        Schema defaultV1 = repository.register(CLUSTER_ID, "orders-value", "AVRO", ORDER_AVRO_V1, List.of());
        Schema groupedV1 = repository.register(CLUSTER_ID, "com.acme:orders-key", "AVRO", ORDER_AVRO_V1, List.of());
        Schema defaultV2 = repository.register(CLUSTER_ID, "orders-value", "AVRO", ORDER_AVRO_V2, List.of());

        assertTrue(repository.exist(CLUSTER_ID, "orders-value"));
        assertTrue(repository.exist(CLUSTER_ID, "com.acme:orders-key"));
        assertFalse(repository.exist(CLUSTER_ID, "does-not-exist"));

        // Paginated list & search
        PagedList<Schema> paged = repository.list(
            CLUSTER_ID, new Pagination(10, URIBuilder.empty(), 1), Optional.empty(), List.of()
        );
        assertEquals(2, paged.size());

        List<String> filtered = repository.all(CLUSTER_ID, Optional.of("acme"), List.of());
        assertEquals(List.of("com.acme:orders-key"), filtered);

        // Compatibility update + read
        repository.updateConfig(CLUSTER_ID, "orders-value", new Schema.Config(Schema.Config.CompatibilityLevelConfig.FULL));
        assertEquals(Schema.Config.CompatibilityLevelConfig.FULL, repository.getConfig(CLUSTER_ID, "orders-value").getCompatibilityLevel());

        // Lookup by ID
        Optional<Schema> foundById = repository.getById(CLUSTER_ID, defaultV2.getId());
        assertTrue(foundById.isPresent());
        assertEquals("orders-value", foundById.get().getSubject());
        assertEquals(2, foundById.get().getVersion());

        // Serializer factory round-trip with ApicurioRecordDeserializer
        SchemaSerializer serializer = serializerFactory.createSerializer(CLUSTER_ID, defaultV2.getId());
        byte[] bytes = serializer.serialize("{\"id\":\"order-42\",\"note\":\"express\"}");
        ApicurioRecordDeserializer deserializer = repository.getApicurioRecordDeserializer(CLUSTER_ID);
        String deserialized = deserializer.deserialize("orders", null, bytes, false, new AvroToJsonSerializer(List.of()));
        assertTrue(deserialized.contains("\"id\":\"order-42\""));
        assertTrue(deserialized.contains("\"note\":\"express\""));

        // Delete version and subject
        assertEquals(1, repository.deleteVersion(CLUSTER_ID, "orders-value", 1));
        assertEquals(1, repository.getAllVersions(CLUSTER_ID, "orders-value").size());

        repository.delete(CLUSTER_ID, "com.acme:orders-key");
        assertFalse(repository.exist(CLUSTER_ID, "com.acme:orders-key"));

        // Verify audit events recorded: 3 registers + 1 compatibility update + 1 version delete + 1 subject delete = 6
        verify(auditModule, times(6)).save(any(SchemaAuditEvent.class));
        assertEquals(defaultV1.getId(), groupedV1.getId());
    }

    @Test
    void schemaComparatorPrioritizesGroupedTopicSubjects() throws Exception {
        Schema unrelated = repository.register(CLUSTER_ID, "aaa-other-value", "AVRO", ORDER_AVRO_V1, List.of());
        Schema groupedMatch = repository.register(CLUSTER_ID, "com.acme:orders-value", "AVRO", ORDER_AVRO_V1, List.of());
        Schema defaultMatch = repository.register(CLUSTER_ID, "orders-value", "AVRO", ORDER_AVRO_V1, List.of());

        List<Schema> sorted = List.of(unrelated, groupedMatch, defaultMatch).stream()
            .sorted(new SchemaComparator("orders", false))
            .toList();

        assertEquals("com.acme:orders-value", sorted.get(0).getSubject());
        assertEquals("orders-value", sorted.get(1).getSubject());
        assertEquals("aaa-other-value", sorted.get(2).getSubject());
    }

    @Test
    void mcpSchemaToolServiceWorksWithApicurio() throws Exception {
        Schema registered = repository.register(CLUSTER_ID, "orders-value", "AVRO", ORDER_AVRO_V1, List.of());
        repository.register(CLUSTER_ID, "com.acme:orders-extra", "AVRO", ORDER_AVRO_V2, List.of());

        AkhqSchemaToolService mcpService = new AkhqSchemaToolService(repository);

        SearchSchemasResult searchResult = mcpService.searchSchemas(CLUSTER_ID, List.of(), "orders", 50);
        assertEquals(2, searchResult.totalMatches());

        GetSchemaResult getResult = mcpService.getSchema(CLUSTER_ID, "orders-value", null);
        assertEquals("orders-value", getResult.subject());
        assertEquals(registered.getId(), getResult.id());
        assertEquals("AVRO", getResult.schemaType());

        FindSchemasForTopicResult topicSchemas = mcpService.findSchemasForTopic(CLUSTER_ID, List.of(), "orders");
        assertNotNull(topicSchemas.valueSchema());
        assertEquals("orders-value", topicSchemas.valueSchema().subject());
        assertEquals(List.of("com.acme:orders-extra"), topicSchemas.otherSubjects());

        GetSchemaByIdResult byId = mcpService.getSchemaById(CLUSTER_ID, List.of(), registered.getId(), "orders");
        assertNotNull(byId.schema());
        assertEquals("orders-value", byId.schema().subject());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
