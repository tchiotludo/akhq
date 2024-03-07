package org.akhq.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.akhq.AbstractTest;
import org.akhq.KafkaClusterExtension;
import org.akhq.KafkaTestCluster;
import org.akhq.models.Config;
import org.akhq.models.audit.TopicAuditEvent;
import org.akhq.repositories.ConfigRepository;
import org.akhq.repositories.RecordRepository;
import org.akhq.repositories.TopicRepository;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(KafkaClusterExtension.class)
@MicronautTest(environments = "audit")
class AuditModuleTest extends AbstractTest {

    private static final String AUDIT_TOPIC_NAME = "audit";

    @Inject
    @InjectMocks
    protected TopicRepository topicRepository;

    @Inject
    @InjectMocks
    protected ConfigRepository configRepository;

    @Inject
    @InjectMocks
    protected RecordRepository recordRepository;

    @Inject
    private KafkaModule kafkaModule;

    @Mock
    ApplicationContext applicationContext;

    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        kafkaModule
            .getAdminClient(KafkaTestCluster.CLUSTER_ID)
            .createTopics(List.of(new NewTopic(AUDIT_TOPIC_NAME, 1, (short) 1)));
    }

    @AfterEach
    void after() {
        kafkaModule
            .getAdminClient(KafkaTestCluster.CLUSTER_ID)
            .deleteTopics(List.of(AUDIT_TOPIC_NAME));
    }

    @Test
    void topicAudit() throws ExecutionException, InterruptedException, IOException {
        String generatedString = generateRandomString();

        topicRepository.create(KafkaTestCluster.CLUSTER_ID, generatedString, 1, (short) 1, Collections.emptyList()
        );

        var consumer = kafkaModule.getConsumer(KafkaTestCluster.CLUSTER_ID);

        assertTrue(consumer.listTopics().keySet().stream().anyMatch(t -> t.equals(AUDIT_TOPIC_NAME)));

        consumer.assign(List.of(new TopicPartition(AUDIT_TOPIC_NAME, 0)));
        consumer.seekToBeginning(List.of(new TopicPartition(AUDIT_TOPIC_NAME, 0)));

        TopicAuditEvent event = null;

        // Creation event tests
        event = getTopicAuditEvent(consumer, TopicAuditEvent.Type.NEW_TOPIC, generatedString);

        assertNotNull(event);
        assertEquals(generatedString, event.getTopicName());
        assertEquals(1, event.getPartitions());
        assertEquals("test", event.getClusterId());

        topicRepository.increasePartition(KafkaTestCluster.CLUSTER_ID, generatedString, 2);

        // Increase partition event tests
        event = getTopicAuditEvent(consumer, TopicAuditEvent.Type.INCREASE_PARTITION, generatedString);

        assertNotNull(event);
        assertEquals(generatedString, event.getTopicName());
        assertEquals(2, event.getPartitions());
        assertEquals("test", event.getClusterId());

        configRepository.updateTopic(KafkaTestCluster.CLUSTER_ID, generatedString, List.of(new Config("max.message.bytes", "2097164")));

        // Configuration change event tests
        event = getTopicAuditEvent(consumer, TopicAuditEvent.Type.CONFIG_CHANGE, generatedString);

        assertNotNull(event);
        assertEquals(generatedString, event.getTopicName());
        assertEquals("2097164", event.getConfig().get("max.message.bytes"));
        assertEquals("test", event.getClusterId());

        topicRepository.delete(KafkaTestCluster.CLUSTER_ID, generatedString);

        // Deletion event tests
        event = getTopicAuditEvent(consumer, TopicAuditEvent.Type.DELETE_TOPIC, generatedString);

        assertNotNull(event);
        assertEquals(generatedString, event.getTopicName());

        consumer.close();
    }

    private TopicAuditEvent getTopicAuditEvent(KafkaConsumer<byte[], byte[]> consumer, TopicAuditEvent.Type type, String generatedString) throws IOException {
        var start = LocalDateTime.now();
        while (true) {
            if (Duration.between(start, LocalDateTime.now()).toSeconds() > 5) {
                return null;
            }
            var records = consumer.poll(Duration.ofSeconds(5));
            for (ConsumerRecord<byte[], byte[]> record : records.records(new TopicPartition(AUDIT_TOPIC_NAME, 0))) {
                var raw = record.value();
                var payload = mapper.readValue(raw, TopicAuditEvent.class);
                if (payload.getType().equals(type) && payload.getTopicName().equals(generatedString)) {
                    return payload;
                }
            }
        }
    }

    private String generateRandomString() {
        int leftLimit = 97;
        int rightLimit = 122;
        int targetStringLength = 10;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        return buffer.toString();
    }

}