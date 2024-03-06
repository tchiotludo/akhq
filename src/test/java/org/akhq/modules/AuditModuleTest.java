package org.akhq.modules;

import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.akhq.AbstractTest;
import org.akhq.KafkaClusterExtension;
import org.akhq.KafkaTestCluster;
import org.akhq.repositories.RecordRepository;
import org.akhq.repositories.TopicRepository;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

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

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
    }

//    @BeforeAll
//    void init() throws ExecutionException, InterruptedException {
//        topicRepository.create(KafkaTestCluster.CLUSTER_ID, "audit", 1, (short) 1, Collections.emptyList());
//    }

    @Test
    void createTopicAudit() throws ExecutionException, InterruptedException {
        String generatedString = generateRandomString();

        topicRepository.create(KafkaTestCluster.CLUSTER_ID, generatedString, 1, (short) 1, Collections.emptyList()
        );

        await().atMost(Duration.ofSeconds(15)).until(() -> {
            try {
                topicRepository.findByName(KafkaTestCluster.CLUSTER_ID, "audit");
                return true;
            } catch (NoSuchElementException | UnknownTopicOrPartitionException e) {
                return false;
            }
        });

        var consumer = kafkaModule.getConsumer(KafkaTestCluster.CLUSTER_ID);
        consumer.assign(List.of(new TopicPartition("audit", 0)));

        ConsumerRecords<byte[], byte[]> records = null;

        var start = LocalDateTime.now();
        while (true) {

            if (Duration.between(start, LocalDateTime.now()).toSeconds() > 30) {
                break;
            }

            records = consumer.poll(Duration.ofSeconds(5));
            if (!records.isEmpty()) {
                break;
            }

        }


        consumer.close();

        assert records != null;
        assertEquals(1, records.count());

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