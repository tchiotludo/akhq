package org.akhq.modules;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.akhq.AbstractTest;
import org.akhq.KafkaClusterExtension;
import org.akhq.KafkaTestCluster;
import org.akhq.models.Topic;
import org.akhq.models.audit.TopicAuditEvent;
import org.akhq.repositories.RecordRepository;
import org.akhq.repositories.TopicRepository;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(KafkaClusterExtension.class)
@MicronautTest(environments = "audit")
class AuditModuleTest extends AbstractTest {

    @Inject
    @InjectMocks
    protected TopicRepository topicRepository;

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
    }

    @BeforeAll
    void init() {
        kafkaModule
            .getAdminClient(KafkaTestCluster.CLUSTER_ID)
            .createTopics(List.of(new NewTopic("audit", 1, (short) 1)));
    }

    @Test
    void createTopicAudit() throws ExecutionException, InterruptedException, IOException {
        String generatedString = generateRandomString();

        topicRepository.create(KafkaTestCluster.CLUSTER_ID, generatedString, 1, (short) 1, Collections.emptyList()
        );

        var consumer = kafkaModule.getConsumer(KafkaTestCluster.CLUSTER_ID);

        assertTrue(consumer.listTopics().keySet().stream().anyMatch(t -> t.equals("audit")));

        consumer.assign(List.of(new TopicPartition("audit", 0)));
        consumer.seekToBeginning(List.of(new TopicPartition("audit", 0)));

        TopicAuditEvent event = null;

        var start = LocalDateTime.now();
        boolean isFound = false;
        while (!isFound) {

            if (Duration.between(start, LocalDateTime.now()).toSeconds() > 5) {
                break;
            }

            var records = consumer.poll(Duration.ofSeconds(5));
            for (ConsumerRecord<byte[], byte[]> record : records.records(new TopicPartition("audit", 1))) {
                var raw = record.value();
                var payload = mapper.readValue(raw, TopicAuditEvent.class);
                if (payload.getTopicName().equals(generatedString)) {
                    event = payload;
                    isFound = true;
                }
            }

        }

        consumer.close();

        assertNotNull(event);
        assertEquals(generatedString, event.getTopicName());
        assertEquals(1, event.getPartitions());
        assertEquals("some", event.getUserName());

        topicRepository.delete(KafkaTestCluster.CLUSTER_ID, generatedString);
    }

    private String generateRandomString() {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
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