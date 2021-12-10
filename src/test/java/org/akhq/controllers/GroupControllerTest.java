package org.akhq.controllers;

import io.micronaut.http.HttpRequest;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.AccessControl;
import org.akhq.models.Consumer;
import org.akhq.models.ConsumerGroup;
import org.akhq.models.TopicPartition;
import org.akhq.modules.KafkaModule;
import org.akhq.repositories.RecordRepository;
import org.akhq.utils.ResultPagedList;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.ConsumerGroupState;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupControllerTest extends AbstractTest {
    public static final String BASE_URL = "/api/" + KafkaTestCluster.CLUSTER_ID + "/group";
    public static final String GROUP_URL = BASE_URL + "/" + KafkaTestCluster.CONSUMER_STREAM_TEST;

    @Inject
    private KafkaModule kafkaModule;

    @Test
    void listApi() {
        ResultPagedList<ConsumerGroup> result;

        result = this.retrievePagedList(HttpRequest.GET(BASE_URL), ConsumerGroup.class);
        assertEquals(5, result.getResults().size());

        result = this.retrievePagedList(HttpRequest.GET(BASE_URL + "?page=2"), ConsumerGroup.class);
        assertEquals(1, result.getResults().size());
        assertEquals("stream-test-example", result.getResults().get(0).getId());
    }

    @Test
    void homeApi() {
        ConsumerGroup result = this.retrieve(HttpRequest.GET(GROUP_URL), ConsumerGroup.class);
        assertEquals("stream-test-example", result.getId());
    }

    @Test
    void offsetsApi() {
        List<TopicPartition.ConsumerGroupOffset> result = this.retrieveList(
            HttpRequest.GET(GROUP_URL + "/offsets"),
            TopicPartition.ConsumerGroupOffset.class
        );
        assertEquals(9, result.size());
    }

    @Test
    void membersApi() {
        List<Consumer> result = this.retrieveList(HttpRequest.GET(GROUP_URL + "/members"), Consumer.class);
        assertEquals(1, result.size());
    }

    @Test
    void offsetsStartApi() {
        List<RecordRepository.TimeOffset> result = this.retrieveList(
            HttpRequest.GET(GROUP_URL + "/offsets/start?timestamp=2020-03-28T11:40:10.123Z"),
            RecordRepository.TimeOffset.class
        );

        assertEquals(4, result.size());
        assertEquals(0, result.get(0).getOffset());
    }

    @Test
    void aclsApi() {
        List<AccessControl> result = this.retrieveList(
            HttpRequest.GET(BASE_URL + "/groupConsumer/acls"),
            AccessControl.class
        );

        assertEquals(2, result.size());
        assertEquals("user:toto", result.get(0).getPrincipal());
        assertEquals("user:tata", result.get(1).getPrincipal());
    }

    @Test
    void consumer() {
        String name = UUID.randomUUID().toString();
        Properties properties = new Properties();
        properties.put("group.id", name);

        ConsumerGroup result;
        KafkaConsumer<byte[], byte[]> consumer = kafkaModule.getConsumer(KafkaTestCluster.CLUSTER_ID, properties);
        consumer.subscribe(Collections.singletonList(KafkaTestCluster.TOPIC_RANDOM));
        consumer.poll(Duration.ofMillis(1000));
        consumer.commitSync();
        consumer.close();

        result = this.retrieve(
            HttpRequest.GET(BASE_URL + "/" + name),
            ConsumerGroup.class
        );
        assertEquals(0, result.getMembers().size());
        assertEquals(0, result.getOffsets().stream().filter(r -> r.getPartition() == 1).findFirst().orElseThrow().getOffsetLag().get());

        this.exchange(
            HttpRequest
                .POST(
                    BASE_URL + "/" + name + "/offsets",
                    Collections.singletonList(new GroupController.OffsetsUpdate(KafkaTestCluster.TOPIC_RANDOM, 0, 5L))
                )
        );

        List<TopicPartition.ConsumerGroupOffset> updated = this.retrieveList(
            HttpRequest.GET(BASE_URL + "/" + name + "/offsets"),
            TopicPartition.ConsumerGroupOffset.class
        );
        assertEquals(95L, updated.stream().filter(r -> r.getPartition() == 0).findFirst().orElseThrow().getOffsetLag().get());

        this.exchange(
            HttpRequest.DELETE(BASE_URL + "/" + name)
        );

        result = this.retrieve(
            HttpRequest.GET(BASE_URL + "/" + name),
            ConsumerGroup.class
        );
        assertEquals(name, result.getId());
        assertEquals(ConsumerGroupState.DEAD, result.getState());
    }
}
