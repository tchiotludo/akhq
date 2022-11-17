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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TopicControllerTest extends AbstractTest {
    public static final String BASE_URL = "/api/" + KafkaTestCluster.CLUSTER_ID + "/topic";
    public static final String DEFAULTS_CONFIGS_URL = "api/topic/defaults-configs";
    public static final String TOPIC_URL = BASE_URL + "/" + KafkaTestCluster.TOPIC_COMPACTED;
    public static final String CREATE_TOPIC_NAME = UUID.randomUUID().toString();
    public static final String CREATE_TOPIC_URL = BASE_URL + "/" + CREATE_TOPIC_NAME;
    public static final int DEFAULT_PAGE_SIZE = 5;

    @Test
    @Order(1)
    void defaultsConfigsApi(){
        Map<String,Integer> result = this.retrieve(HttpRequest.GET(DEFAULTS_CONFIGS_URL), Map.class);

        assertEquals(1, result.get("replication"));
        assertEquals(86400000, result.get("retention"));
        assertEquals(1, result.get("partition"));
    }


    @Test
    @Order(1)
    void listApi() {
        ResultPagedList<Topic> result;

        int expectedPageCount = (int) Math.ceil((double)KafkaTestCluster.TOPIC_HIDE_INTERNAL_COUNT / DEFAULT_PAGE_SIZE);
        result = this.retrievePagedList(HttpRequest.GET(BASE_URL), Topic.class);
        assertEquals(expectedPageCount, result.getPage());
        assertEquals(DEFAULT_PAGE_SIZE, result.getResults().size());

        result = this.retrievePagedList(HttpRequest.GET(BASE_URL + "?page=2"), Topic.class);
        assertEquals(DEFAULT_PAGE_SIZE, result.getResults().size());

        int expectedLastPageSize = KafkaTestCluster.TOPIC_HIDE_INTERNAL_COUNT - 2 * DEFAULT_PAGE_SIZE;
        result = this.retrievePagedList(HttpRequest.GET(BASE_URL + "?page=3"), Topic.class);
        assertEquals(expectedLastPageSize, result.getResults().size());
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
        assertEquals(KafkaTestCluster.CONSUMER_GROUP_COUNT, result.size());
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
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("value", "my-value");
        paramMap.put("key", "my-key");
        paramMap.put("partition", 1);
        paramMap.put("headers", List.of(
            new KeyValue<>("my-header-1", "1"),
            new KeyValue<>("my-header-2", "2")));
        paramMap.put("multiMessage", false);
        List<Record> response = this.retrieveList(HttpRequest.POST(
            CREATE_TOPIC_URL + "/data", paramMap
        ), Record.class);

        assertEquals(1, response.size());
        assertEquals("my-key", response.get(0).getKey());
        assertEquals("my-value", response.get(0).getValue());
        assertEquals(1, response.get(0).getPartition());
        assertEquals(2, response.get(0).getHeaders().size());
        assertEquals("1", response.get(0).getHeaders().get(0).getValue());
    }

    @Test
    @Order(3)
    void produceTombstone() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("value", null);
        paramMap.put("key", "my-key-tomb");
        paramMap.put("multiMessage", false);
        List<Record> response = this.retrieveList(HttpRequest.POST(
            CREATE_TOPIC_URL + "/data", paramMap
        ), Record.class);

        assertEquals(1, response.size());
        assertEquals("my-key-tomb", response.get(0).getKey());
        assertNull(response.get(0).getValue());
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
        assertEquals(2, retrieve.getOffset());

        // get data
        // @TODO: Failed to see the message
        // records = this.retrieveNextList(HttpRequest.GET(CREATE_TOPIC_URL + "/data"), Record.class);
        // assertEquals(2, records.getResults().size());
        // assertEquals("my-value", records.getResults().get(0).getValue());
        // assertNull(records.getResults().get(1).getValue());
    }

    @Test
    @Order(6)
    void produceMultipleMessages() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("value", "key1_{\"test_1\":1}\n"
                            + "key2_{\"test_1\":2}\n"
                            + "key3_{\"test_1\":3}");
        paramMap.put("multiMessage", true);
        paramMap.put("keyValueSeparator", "_");
        List<Record> response = this.retrieveList(HttpRequest.POST(
            CREATE_TOPIC_URL + "/data", paramMap
        ), Record.class);

        assertEquals(3, response.size());
        assertTrue(response.get(0).getValue().contains("key1_{\"test_1\":1}"));
        assertTrue(response.get(1).getValue().contains("key2_{\"test_1\":2}"));
        assertTrue(response.get(2).getValue().contains("key3_{\"test_1\":3}"));
    }

    @Test
    @Order(8)
    void delete() {
        this.exchange(
            HttpRequest.DELETE(CREATE_TOPIC_URL)
        );
    }
}
