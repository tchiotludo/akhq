package org.akhq.controllers;

import com.google.common.collect.ImmutableMap;
import io.micronaut.http.HttpRequest;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.*;
import org.akhq.utils.ResultNextList;
import org.akhq.utils.ResultPagedList;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TopicControllerTest extends AbstractTest {
    public static final String BASE_URL = "/api/" + KafkaTestCluster.CLUSTER_ID + "/topic";
    public static final String TOPIC_URL = BASE_URL + "/" + KafkaTestCluster.TOPIC_COMPACTED;

    @Test
    void listApi() {
        ResultPagedList<Topic> result;

        result = this.retrievePagedList(HttpRequest.GET(BASE_URL), Topic.class);
        assertEquals(5, result.getResults().size());

        result = this.retrievePagedList(HttpRequest.GET(BASE_URL + "?page=2"), Topic.class);
        assertEquals(KafkaTestCluster.TOPIC_HIDE_INTERNAL_COUNT - 5, result.getResults().size());
        assertEquals("stream-test-example-count-repartition", result.getResults().get(4).getName());
    }

    @Test
    void homeApi() {
        Topic result = this.retrieve(HttpRequest.GET(TOPIC_URL), Topic.class);
        assertEquals(KafkaTestCluster.TOPIC_COMPACTED, result.getName());
    }

    @Test
    void partitionsApi() {
        List<Partition> result = this.retrieveList(HttpRequest.GET(TOPIC_URL + "/partitions"), Partition.class);
        assertEquals(3, result.size());
    }

    @Test
    void groupsApi() {
        List<ConsumerGroup> result = this.retrieveList(HttpRequest.GET(TOPIC_URL + "/groups"), ConsumerGroup.class);
        assertEquals(5, result.size());
    }

    @Test
    void configApi() {
        List<Config> result = this.retrieveList(HttpRequest.GET(TOPIC_URL + "/configs"), Config.class);
        assertEquals("0.0", result.stream().filter(config -> config.getName().equals(TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG)).findFirst().orElseThrow().getValue());
    }

    @Test
    void logsApi() {
        List<LogDir> result = this.retrieveList(HttpRequest.GET(TOPIC_URL + "/logs"), LogDir.class);
        assertEquals(3, result.size());
    }

    @Test
    @Disabled("TODO: rewamp ACL to be api friendly")
    void aclsApi() {
        List<AccessControl> result = this.retrieveList(HttpRequest.GET(BASE_URL + "/testAclTopic/acls"), AccessControl.class);

        assertEquals(2, result.size());
        assertEquals("user:tata", result.get(0).getPrincipal());
    }

    @Test
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
    void createProduceDeleteApi() {
        String name = UUID.randomUUID().toString();
        ResultNextList<Record> records;

        // create
        Topic result = this.retrieve(HttpRequest.POST(
            BASE_URL,
            ImmutableMap.of(
                "name", name,
                "partition", 3,
                "configs",  ImmutableMap.of(
                    TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT
                )
            )
        ), Topic.class);

        assertEquals(name, result.getName());
        assertEquals(3, result.getPartitions().size());

        // check configs
        List<Config> configs = this.retrieveList(HttpRequest.GET(BASE_URL + "/" + name + "/configs"), Config.class);
        assertEquals(TopicConfig.CLEANUP_POLICY_COMPACT, configs.stream().filter(config -> config.getName().equals(TopicConfig.CLEANUP_POLICY_CONFIG)).findFirst().orElseThrow().getValue());

        // produce
        Record record = this.retrieve(HttpRequest.POST(
            BASE_URL + "/" + name + "/data",
            ImmutableMap.of(
                "value", "my-value",
                "key", "my-key",
                "partition", 1,
                "headers",  ImmutableMap.of(
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

        // get data
        records = this.retrieveNextList(HttpRequest.GET(BASE_URL + "/" + name + "/data"), Record.class);
        assertEquals(1, records.getResults().size());
        assertEquals("my-value", records.getResults().get(0).getValue());

        // delete data
        Record retrieve = this.retrieve(
            HttpRequest.DELETE(
                BASE_URL + "/" + name + "/data",
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
        // records = this.retrieveNextList(HttpRequest.GET(BASE_URL + "/" + name + "/data"), Record.class);
        // assertEquals(2, records.getResults().size());
        // assertEquals("my-value", records.getResults().get(0).getValue());
        // assertNull(records.getResults().get(1).getValue());

        // delete topic
        this.exchange(
            HttpRequest.DELETE(BASE_URL + "/" + name)
        );
    }
}
