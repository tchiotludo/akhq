package org.akhq.controllers;

import com.google.common.collect.ImmutableMap;
import io.micronaut.http.HttpRequest;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.*;
import org.akhq.utils.ResultNextList;
import org.akhq.utils.ResultPagedList;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.*;
import org.akhq.models.Record;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TopicControllerTest extends AbstractTest {
    public static final String BASE_URL = "/api/" + KafkaTestCluster.CLUSTER_ID + "/topic";
    public static final String TOPIC_URL = BASE_URL + "/" + KafkaTestCluster.TOPIC_COMPACTED;
    public static final String CREATE_TOPIC_NAME = UUID.randomUUID().toString();
    public static final String CREATE_TOPIC_URL = BASE_URL + "/" + CREATE_TOPIC_NAME;

    @Test
    @Order(1)
    void listApi() {
        ResultPagedList<Topic> result;

        result = this.retrievePagedList(HttpRequest.GET(BASE_URL), Topic.class);
        assertEquals(5, result.getResults().size());

        result = this.retrievePagedList(HttpRequest.GET(BASE_URL + "?page=2"), Topic.class);
        assertEquals(KafkaTestCluster.TOPIC_HIDE_INTERNAL_COUNT - 6, result.getResults().size());
        assertEquals("stream-test-example-count-changelog", result.getResults().get(4).getName());
    }

    @Test
    @Order(1)
    void homeApi() {
        Topic result = this.retrieve(HttpRequest.GET(TOPIC_URL), Topic.class);
        assertEquals(KafkaTestCluster.TOPIC_COMPACTED, result.getName());
    }

    @Test
    @Order(1)
    void partitionsApi() {
        List<Partition> result = this.retrieveList(HttpRequest.GET(TOPIC_URL + "/partitions"), Partition.class);
        assertEquals(3, result.size());
    }

    @Test
    @Order(1)
    void groupsApi() {
        List<ConsumerGroup> result = this.retrieveList(HttpRequest.GET(TOPIC_URL + "/groups"), ConsumerGroup.class);
        assertEquals(5, result.size());
    }

    @Test
    @Order(1)
    void configApi() {
        List<Config> result = this.retrieveList(HttpRequest.GET(TOPIC_URL + "/configs"), Config.class);
        assertEquals("0.0", result.stream().filter(config -> config.getName().equals(TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG)).findFirst().orElseThrow().getValue());
    }

    @Test
    @Order(1)
    void logsApi() {
        List<LogDir> result = this.retrieveList(HttpRequest.GET(TOPIC_URL + "/logs"), LogDir.class);
        assertEquals(3, result.size());
    }

    @Test
    @Order(1)
    void aclsApi() {
        List<AccessControl> result = this.retrieveList(HttpRequest.GET(BASE_URL + "/testAclTopic/acls"), AccessControl.class);

        assertEquals(2, result.size());
        assertEquals("user:toto", result.get(0).getPrincipal());
        assertEquals("user:tata", result.get(1).getPrincipal());
    }

    @Test
    @Order(1)
    void updateConfigApi() {
        String s = String.valueOf(new Random().nextInt((Integer.MAX_VALUE - Integer.MAX_VALUE/2) + 1) + Integer.MAX_VALUE/2);

        List<Config> result = this.retrieveList(
            HttpRequest.POST(
                TOPIC_URL + "/configs",
                ImmutableMap.of("message.timestamp.difference.max.ms", s)
            ),
            Config.class
        );

        assertEquals(s, result.stream().filter(config -> config.getName().equals("message.timestamp.difference.max.ms")).findFirst().orElseThrow().getValue());
    }

    @Test
    @Order(1)
    void dataApi() {
        ResultNextList<Record> result;
        String after = TOPIC_URL + "/data";

        int count = 0;
        while (after != null) {
            result = this.retrieveNextList(HttpRequest.GET(after), Record.class);
            assertEquals(300, result.getSize());
            if (result.getResults() != null) {
                count = count + result.getResults().size();
            }
            after = result.getAfter();
        }

        assertEquals(153, count);
    }

    @Test
    @Order(2)
    void create() {
        // create
        Topic result = this.retrieve(HttpRequest.POST(
            BASE_URL,
            ImmutableMap.of(
                "name", CREATE_TOPIC_NAME,
                "partition", 3,
                "configs", ImmutableMap.of(
                    TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT
                )
            )
        ), Topic.class);

        assertEquals(CREATE_TOPIC_NAME, result.getName());
        assertEquals(3, result.getPartitions().size());
    }

    @Test
    @Order(3)
    void checkConfigs() {
        List<Config> configs = this.retrieveList(HttpRequest.GET(CREATE_TOPIC_URL + "/configs"), Config.class);
        assertEquals(
            TopicConfig.CLEANUP_POLICY_COMPACT,
            configs.stream()
                .filter(config -> config.getName().equals(TopicConfig.CLEANUP_POLICY_CONFIG))
                .findFirst()
                .orElseThrow()
                .getValue()
        );
    }

    @Test
    @Order(3)
    void produce() {
        Record record = this.retrieve(HttpRequest.POST(
            CREATE_TOPIC_URL + "/data",
            ImmutableMap.of(
                "value", "my-value",
                "key", "my-key",
                "partition", 1,
                "headers", ImmutableMap.of(
                    "my-header-1", "1",
                    "my-header-2", "2"
                )
            )
        ), Record.class);

        assertEquals("my-key", record.getKey());
        assertEquals("my-value", record.getValue());
        assertEquals(1, record.getPartition());
        assertEquals(2, record.getHeaders().size());
        assertEquals("1", record.getHeaders().get("my-header-1"));
    }

    @Test
    @Order(4)
    void dataGet() {
        ResultNextList<Record> records = this.retrieveNextList(HttpRequest.GET(CREATE_TOPIC_URL + "/data"), Record.class);
        assertEquals(1, records.getResults().size());
        assertEquals("my-value", records.getResults().get(0).getValue());
    }

    @Test
    @Order(5)
    void dataDelete() {
        Record retrieve = this.retrieve(
            HttpRequest.DELETE(
                CREATE_TOPIC_URL + "/data",
                ImmutableMap.of(
                    "key", new String(Base64.getEncoder().encode("my-key".getBytes())),
                    "partition", 1
                )
            ),
            Record.class
        );
        assertEquals(1, retrieve.getOffset());

        // get data
        // @TODO: Failed to see the message
        // records = this.retrieveNextList(HttpRequest.GET(CREATE_TOPIC_URL + "/data"), Record.class);
        // assertEquals(2, records.getResults().size());
        // assertEquals("my-value", records.getResults().get(0).getValue());
        // assertNull(records.getResults().get(1).getValue());
    }

    @Test
    @Order(6)
    void delete() {
        this.exchange(
            HttpRequest.DELETE(CREATE_TOPIC_URL)
        );
    }
}
