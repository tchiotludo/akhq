package org.akhq.modules.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.akhq.AbstractTest;
import org.akhq.KafkaClusterExtension;
import org.akhq.KafkaTestCluster;
import org.akhq.models.Schema;
import org.akhq.models.audit.AuditEvent;
import org.akhq.models.audit.SchemaAuditEvent;
import org.akhq.modules.KafkaModule;
import org.akhq.repositories.SchemaRegistryRepository;
import org.apache.avro.SchemaBuilder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(KafkaClusterExtension.class)
@MicronautTest(environments = "audit")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SchemaAuditModuleTest extends AbstractTest {

    private static final String AUDIT_TOPIC_NAME = "audit";

    @Inject
    private KafkaModule kafkaModule;

    @Inject
    private SchemaRegistryRepository repository;

    private final ObjectMapper mapper = new ObjectMapper();

    public final static String SUBJECT = "AUDIT_";
    public final static org.apache.avro.Schema SCHEMA_V1 = SchemaBuilder
        .record("auditSchema").namespace("org.akhq")
        .fields()
        .name("clientHash").type().fixed("MD5").size(16).noDefault()
        .name("clientProtocol").type().nullable().stringType().noDefault()
        .name("serverHash").type("MD5").noDefault()
        .name("meta").type().nullable().map().values().bytesType().noDefault()
        .endRecord();

    public final static org.apache.avro.Schema SCHEMA_V2 = SchemaBuilder
        .record("auditSchema").namespace("org.akhq")
        .fields()
        .name("clientHash").type().fixed("MD5").size(16).noDefault()
        .name("clientProtocol").type().nullable().stringType().noDefault()
        .name("serverHash").type("MD5").noDefault()
        .name("meta").type().nullable().map().values().bytesType().noDefault()
        .name("serverIp").type().nullable().stringType().noDefault()
        .endRecord();

    @Test
    @Order(1)
    public void createSchema() throws RestClientException, IOException {
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT, SCHEMA_V1.toString(), Collections.emptyList());
        var event = (SchemaAuditEvent) searchAuditEvent(AuditEvent.ActionType.SCHEMA_CREATE, SUBJECT);
        assertNotNull(event);
        assertNotNull(event.getSchemaId());
        assertEquals(1, event.getVersion());
    }

    @Test
    @Order(2)
    public void updateCompatibilitySchema() throws RestClientException, IOException {
        repository.updateConfig(KafkaTestCluster.CLUSTER_ID, SUBJECT, new Schema.Config(Schema.Config.CompatibilityLevelConfig.FORWARD));
        var event = (SchemaAuditEvent) searchAuditEvent(AuditEvent.ActionType.SCHEMA_COMPATIBILITY_UPDATE, SUBJECT);
        assertNotNull(event);
        assertEquals(Schema.Config.CompatibilityLevelConfig.FORWARD.name(), event.getNewCompatibility());
    }

    @Test
    @Order(3)
    public void updateSchema() throws RestClientException, IOException {
        repository.register(KafkaTestCluster.CLUSTER_ID, SUBJECT, SCHEMA_V2.toString(), Collections.emptyList());
        var event = (SchemaAuditEvent) searchAuditEvent(AuditEvent.ActionType.SCHEMA_UPDATE, SUBJECT);
        assertNotNull(event);
        assertNotNull(event.getSchemaId());
        assertEquals(2, event.getVersion());
    }

    @Test
    @Order(4)
    public void deleteSchema() throws RestClientException, IOException {
        repository.delete(KafkaTestCluster.CLUSTER_ID, SUBJECT);
        var event = (SchemaAuditEvent) searchAuditEvent(AuditEvent.ActionType.SCHEMA_DELETE, SUBJECT);
        assertNotNull(event);
    }

    private AuditEvent searchAuditEvent(AuditEvent.ActionType actionType, String schemaName) throws IOException {
        var consumer = kafkaModule.getConsumer(KafkaTestCluster.CLUSTER_ID);
        consumer.assign(List.of(new TopicPartition(AUDIT_TOPIC_NAME, 0)));
        consumer.seekToBeginning(List.of(new TopicPartition(AUDIT_TOPIC_NAME, 0)));

        var start = LocalDateTime.now();
        while (true) {
            if (Duration.between(start, LocalDateTime.now()).toSeconds() > 5) {
                consumer.close();
                return null;
            }
            var records = consumer.poll(Duration.ofSeconds(5));
            for (ConsumerRecord<byte[], byte[]> record : records.records(new TopicPartition(AUDIT_TOPIC_NAME, 0))) {
                var raw = record.value();

                var payload = mapper.readValue(raw, AuditEvent.class);

                if (!payload.getType().equals("SCHEMA")) continue;

                var event = (SchemaAuditEvent) payload;

                if (event.getSubject().equals(schemaName) && event.getActionType().equals(actionType)) {
                    consumer.close();
                    return event;
                }

            }
        }
    }

}
