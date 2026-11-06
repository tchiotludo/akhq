package org.akhq.modules.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.akhq.AbstractTest;
import org.akhq.KafkaClusterExtension;
import org.akhq.KafkaTestCluster;
import org.akhq.models.Config;
import org.akhq.models.audit.AuditEvent;
import org.akhq.models.audit.ConsumerGroupAuditEvent;
import org.akhq.models.audit.RecordAuditEvent;
import org.akhq.models.audit.TopicAuditEvent;
import org.akhq.modules.KafkaModule;
import org.akhq.repositories.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static org.akhq.models.audit.AuditEvent.ActionType.CONSUMER_GROUP_UPDATE_OFFSETS;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(KafkaClusterExtension.class)
@MicronautTest(environments = "audit")
class KafkaAuditModuleTest extends AbstractTest {

    private static final String AUDIT_TOPIC_NAME = "audit";

    @Inject
    @InjectMocks
    protected TopicRepository topicRepository;

    @Inject
    @InjectMocks
    protected ConfigRepository configRepository;

    @Inject
    @InjectMocks
    protected ConsumerGroupRepository consumerGroupRepository;

    @Inject
    @InjectMocks
    protected RecordRepository recordRepository;

    @Inject
    private KafkaModule kafkaModule;

    @Mock
    ApplicationContext applicationContext;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void topicAudit() throws ExecutionException, InterruptedException, IOException {
        String generatedString = generateRandomString();

        topicRepository.create(KafkaTestCluster.CLUSTER_ID, generatedString, 1, (short) 1, Collections.emptyList()
        );

        TopicAuditEvent event = null;

        // Creation event tests
        event = (TopicAuditEvent) searchAuditEvent(AuditEvent.ActionType.TOPIC_CREATE, generatedString);

        assertNotNull(event);
        assertEquals(generatedString, event.getTopicName());
        assertEquals(1, event.getPartitions());
        assertEquals("test", event.getClusterId());

        topicRepository.increasePartition(KafkaTestCluster.CLUSTER_ID, generatedString, 2);

        // Increase partition event tests
        event = (TopicAuditEvent) searchAuditEvent(AuditEvent.ActionType.TOPIC_INCREASE_PARTITION, generatedString);

        assertNotNull(event);
        assertEquals(generatedString, event.getTopicName());
        assertEquals(2, event.getPartitions());
        assertEquals("test", event.getClusterId());

        configRepository.updateTopic(KafkaTestCluster.CLUSTER_ID, generatedString, List.of(new Config("max.message.bytes", "2097164")));

        // Configuration change event tests
        event = (TopicAuditEvent) searchAuditEvent(AuditEvent.ActionType.TOPIC_CONFIG_CHANGE, generatedString);

        assertNotNull(event);
        assertEquals(generatedString, event.getTopicName());
        assertEquals("2097164", event.getConfig().get("max.message.bytes"));
        assertEquals("test", event.getClusterId());

        topicRepository.delete(KafkaTestCluster.CLUSTER_ID, generatedString);

        // Deletion event tests
        event = (TopicAuditEvent) searchAuditEvent(AuditEvent.ActionType.TOPIC_DELETE, generatedString);

        assertNotNull(event);
        assertEquals(generatedString, event.getTopicName());
    }

    @Test
    public void consumerGroupAudit() throws ExecutionException, InterruptedException, IOException {
        String generatedString = generateRandomString();

        topicRepository.create(KafkaTestCluster.CLUSTER_ID, generatedString, 1, (short) 1, Collections.emptyList()
        );


        var producer = kafkaModule.getProducer(KafkaTestCluster.CLUSTER_ID);
        for (int i = 0; i < 100; i++) {
            producer.send(new ProducerRecord<>(generatedString, new byte[]{})).get();
        }

        var consumer = kafkaModule.getConsumer(KafkaTestCluster.CLUSTER_ID);
        consumer.assign(List.of(new TopicPartition(generatedString, 0)));
        consumer.commitSync(Map.of(new TopicPartition(generatedString, 0), new OffsetAndMetadata(99)));
        consumer.close();

        var consumerGroup = consumerGroupRepository.findByTopic(KafkaTestCluster.CLUSTER_ID, generatedString, Collections.emptyList());

        assertTrue(consumerGroup.get(0).getOffsets().get(0).getOffset().isPresent());
        assertEquals(99, consumerGroup.get(0).getOffsets().get(0).getOffset().get());

        // Update consumer group
        consumerGroupRepository.updateOffsets(KafkaTestCluster.CLUSTER_ID,
            consumerGroup.get(0).getId(),
            Map.of(new org.akhq.models.TopicPartition(generatedString, 0), 0L)
        );

        var event = (ConsumerGroupAuditEvent) searchAuditEvent(CONSUMER_GROUP_UPDATE_OFFSETS, generatedString);

        assertNotNull(event);
        assertNotNull(event.getConsumerGroupName());
        assertEquals(KafkaTestCluster.CLUSTER_ID, event.getClusterId());
        assertEquals(generatedString, event.getTopic());

    }

    @Test
    void recordAudit() throws ExecutionException, InterruptedException, IOException, RestClientException {
        String generatedString = generateRandomString();

        topicRepository.create(KafkaTestCluster.CLUSTER_ID, generatedString, 1, (short) 1, Collections.emptyList());

        // Single produce event tests
        recordRepository.produce(
            KafkaTestCluster.CLUSTER_ID,
            generatedString,
            Optional.of("value"),
            Collections.emptyList(),
            Optional.of("key"),
            Optional.of(0),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            false,
            Optional.empty()
        );

        var produceEvent = (RecordAuditEvent) searchAuditEvent(AuditEvent.ActionType.RECORD_PRODUCE, generatedString);

        assertNotNull(produceEvent);
        assertEquals(generatedString, produceEvent.getTopicName());
        assertEquals(KafkaTestCluster.CLUSTER_ID, produceEvent.getClusterId());
        // The produce event records the action, not a per-record partition (records may span partitions)
        assertNull(produceEvent.getPartition());
        assertEquals(1, produceEvent.getRecordCount());

        // Record delete event tests
        recordRepository.delete(KafkaTestCluster.CLUSTER_ID, generatedString, 0, "key".getBytes());

        var deleteEvent = (RecordAuditEvent) searchAuditEvent(AuditEvent.ActionType.RECORD_DELETE, generatedString);

        assertNotNull(deleteEvent);
        assertEquals(generatedString, deleteEvent.getTopicName());
        assertEquals(KafkaTestCluster.CLUSTER_ID, deleteEvent.getClusterId());
        assertEquals(0, deleteEvent.getPartition());

        // Empty topic event tests
        recordRepository.emptyTopic(KafkaTestCluster.CLUSTER_ID, generatedString);

        var emptyEvent = (RecordAuditEvent) searchAuditEvent(AuditEvent.ActionType.RECORD_EMPTY, generatedString);

        assertNotNull(emptyEvent);
        assertEquals(generatedString, emptyEvent.getTopicName());
        assertEquals(KafkaTestCluster.CLUSTER_ID, emptyEvent.getClusterId());

        // A second topic to cover the multi-message produce and empty-by-timestamp paths
        String multiTopic = generateRandomString();
        topicRepository.create(KafkaTestCluster.CLUSTER_ID, multiTopic, 1, (short) 1, Collections.emptyList());

        // Multi-message produce: recordCount must equal the number of split key/value pairs
        recordRepository.produce(
            KafkaTestCluster.CLUSTER_ID,
            multiTopic,
            Optional.of("key1=value1\nkey2=value2"),
            Collections.emptyList(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            true,
            Optional.of("=")
        );

        var multiProduceEvent = (RecordAuditEvent) searchAuditEvent(AuditEvent.ActionType.RECORD_PRODUCE, multiTopic);

        assertNotNull(multiProduceEvent);
        assertEquals(multiTopic, multiProduceEvent.getTopicName());
        assertNull(multiProduceEvent.getPartition());
        assertEquals(2, multiProduceEvent.getRecordCount());

        // Empty by timestamp must also emit a RECORD_EMPTY event
        recordRepository.emptyTopicByTimestamp(KafkaTestCluster.CLUSTER_ID, multiTopic, 0L);

        var emptyByTimestampEvent = (RecordAuditEvent) searchAuditEvent(AuditEvent.ActionType.RECORD_EMPTY, multiTopic);

        assertNotNull(emptyByTimestampEvent);
        assertEquals(multiTopic, emptyByTimestampEvent.getTopicName());
        assertEquals(KafkaTestCluster.CLUSTER_ID, emptyByTimestampEvent.getClusterId());
    }

    private AuditEvent searchAuditEvent(AuditEvent.ActionType actionType, String topicName) throws IOException {
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

                if (payload.getType().equals("TOPIC")) {
                    var event = (TopicAuditEvent) payload;
                    if (event.getTopicName().equals(topicName) && event.getActionType().equals(actionType)) {
                        consumer.close();
                        return payload;
                    }
                } else if (payload.getType().equals("RECORD")) {
                    var event = (RecordAuditEvent) payload;
                    if (event.getTopicName().equals(topicName) && event.getActionType().equals(actionType)) {
                        consumer.close();
                        return payload;
                    }
                } else if (payload.getType().equals("CONSUMER_GROUP")) {
                    var event = (ConsumerGroupAuditEvent) payload;
                    if (event.getActionType().equals(actionType)) {
                        if (topicName == null) {
                            if (event.getTopic() == null) {
                                consumer.close();
                                return payload;
                            }
                        } else {
                            if (event.getTopic().equals(topicName)) {
                                consumer.close();
                                return payload;
                            }
                        }
                    }
                }
            }
        }
    }

    private String generateRandomString() {
        int leftLimit = 97;
        int rightLimit = 122;
        int targetStringLength = 8;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        return "generated_" + buffer; // we mark it as internal so that we don't interfere we other tests
    }

}