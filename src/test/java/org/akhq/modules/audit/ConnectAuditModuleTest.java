package org.akhq.modules.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.akhq.AbstractTest;
import org.akhq.KafkaClusterExtension;
import org.akhq.KafkaTestCluster;
import org.akhq.models.audit.AuditEvent;
import org.akhq.models.audit.ConnectAuditEvent;
import org.akhq.modules.KafkaModule;
import org.akhq.repositories.ConnectRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@ExtendWith(KafkaClusterExtension.class)
@MicronautTest(environments = "audit")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConnectAuditModuleTest extends AbstractTest {

    private static final String AUDIT_TOPIC_NAME = "audit";

    @Inject
    private KafkaModule kafkaModule;

    private final ObjectMapper mapper = new ObjectMapper();

    @Inject
    @InjectMocks
    private ConnectRepository repository;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @Order(1)
    public void create() throws IOException {
        String path1 = "/tmp/file1.data";

        repository.create(
            KafkaTestCluster.CLUSTER_ID,
            "connect-1",
            "connectAuditTest",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", path1,
                "topics", KafkaTestCluster.TOPIC_CONNECT
            )
        );

        var event = (ConnectAuditEvent) searchAuditEvent(AuditEvent.ActionType.CONNECT_CREATE, "connectAuditTest");
        assertNotNull(event);
        assertEquals(KafkaTestCluster.CLUSTER_ID, event.getKafkaClusterId());
        assertEquals("connect-1", event.getConnectClusterId());
    }

    @Test
    @Order(2)
    public void update() throws IOException {
        String path2 = "/tmp/file2.data";

        repository.update(
            KafkaTestCluster.CLUSTER_ID,
            "connect-1",
            "connectAuditTest",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", path2,
                "topics", KafkaTestCluster.TOPIC_CONNECT
            )
        );

        var event = (ConnectAuditEvent) searchAuditEvent(AuditEvent.ActionType.CONNECT_UPDATE, "connectAuditTest");
        assertNotNull(event);
        assertEquals(KafkaTestCluster.CLUSTER_ID, event.getKafkaClusterId());
        assertEquals("connect-1", event.getConnectClusterId());
    }

    @Test
    @Order(3)
    public void restart() throws IOException {
        repository.restart(KafkaTestCluster.CLUSTER_ID, "connect-1", "connectAuditTest");

        var event = (ConnectAuditEvent) searchAuditEvent(AuditEvent.ActionType.CONNECT_RESTART, "connectAuditTest");
        assertNotNull(event);
        assertEquals(KafkaTestCluster.CLUSTER_ID, event.getKafkaClusterId());
        assertEquals("connect-1", event.getConnectClusterId());
    }

    @Test
    @Order(4)
    public void restartTask() throws IOException {
        repository.restartTask(KafkaTestCluster.CLUSTER_ID, "connect-1", "connectAuditTest", 0);

        var event = (ConnectAuditEvent) searchAuditEvent(AuditEvent.ActionType.CONNECT_TASK_RESTART, "connectAuditTest");
        assertNotNull(event);
        assertEquals(KafkaTestCluster.CLUSTER_ID, event.getKafkaClusterId());
        assertEquals("connect-1", event.getConnectClusterId());
        assertEquals(0, event.getTaskId());
    }

    @Test
    @Order(5)
    public void pause() throws IOException {
        repository.pause(KafkaTestCluster.CLUSTER_ID, "connect-1", "connectAuditTest");

        var event = (ConnectAuditEvent) searchAuditEvent(AuditEvent.ActionType.CONNECT_PAUSE, "connectAuditTest");
        assertNotNull(event);
        assertEquals(KafkaTestCluster.CLUSTER_ID, event.getKafkaClusterId());
        assertEquals("connect-1", event.getConnectClusterId());
    }

    @Test
    @Order(6)
    public void resume() throws IOException {
        repository.resume(KafkaTestCluster.CLUSTER_ID, "connect-1", "connectAuditTest");

        var event = (ConnectAuditEvent) searchAuditEvent(AuditEvent.ActionType.CONNECT_RESUME, "connectAuditTest");
        assertNotNull(event);
        assertEquals(KafkaTestCluster.CLUSTER_ID, event.getKafkaClusterId());
        assertEquals("connect-1", event.getConnectClusterId());
    }

    @Test
    @Order(7)
    public void delete() throws IOException {
        repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-1", "connectAuditTest");

        var event = (ConnectAuditEvent) searchAuditEvent(AuditEvent.ActionType.CONNECT_DELETE, "connectAuditTest");
        assertNotNull(event);
        assertEquals(KafkaTestCluster.CLUSTER_ID, event.getKafkaClusterId());
        assertEquals("connect-1", event.getConnectClusterId());
    }

    private AuditEvent searchAuditEvent(AuditEvent.ActionType actionType, String connectorName) throws IOException {
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

                if (!payload.getType().equals("CONNECT")) continue;

                var event = (ConnectAuditEvent) payload;

                if (event.getConnectorName().equals(connectorName) && event.getActionType().equals(actionType)) {
                    consumer.close();
                    return event;
                }

            }
        }
    }

}
