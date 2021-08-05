package org.akhq.controllers;

import com.google.common.collect.ImmutableMap;
import io.micronaut.http.HttpRequest;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.ConnectDefinition;
import org.akhq.models.ConnectPlugin;
import org.akhq.repositories.ConnectRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConnectControllerTest extends AbstractTest {
    public static final String BASE_URL = "/api/" + KafkaTestCluster.CLUSTER_ID + "/connect/connect-1";
    public static final String CONNECT_NAME = "connect-controller-test";
    public static final String CONNECT_URL = BASE_URL + "/" + CONNECT_NAME;
    public static final String PATH1 = Objects.requireNonNull(ConnectRepository.class.getClassLoader().getResource("application.yml")).getPath();
    public static final String PATH2 = Objects.requireNonNull(ConnectRepository.class.getClassLoader().getResource("logback.xml")).getPath();

    @Test
    @Order(1)
    void createApi() {
        ConnectDefinition result = this.retrieve(HttpRequest.POST(
            BASE_URL,
            ImmutableMap.of(
                "name", CONNECT_NAME,
                "configs",              ImmutableMap.of(
                    "connector.class", "FileStreamSinkConnector",
                    "file", PATH1,
                    "topics", KafkaTestCluster.TOPIC_CONNECT
                )
            )
        ), ConnectDefinition.class);

        assertEquals(CONNECT_NAME, result.getName());
        assertEquals(PATH1, result.getConfigs().entrySet().stream().filter(r -> r.getKey().equals("file")).findFirst().orElseThrow().getValue());
    }

    @Test
    @Order(2)
    void listApi() {
        List<ConnectDefinition> result = this.retrieveList(HttpRequest.GET(BASE_URL), ConnectDefinition.class);
        assertEquals(1, result.size());
    }

    @Test
    @Order(2)
    void homeApi() {
        ConnectDefinition result = this.retrieve(HttpRequest.GET(CONNECT_URL), ConnectDefinition.class);
        assertEquals(CONNECT_NAME, result.getName());
    }

    @Test
    @Order(2)
    void tasksApi() {
        List<ConnectDefinition.TaskDefinition> result = this.retrieveList(HttpRequest.GET(CONNECT_URL + "/tasks"), ConnectDefinition.TaskDefinition.class);
        assertEquals(1, result.size());
    }

    @SuppressWarnings("rawtypes")
    @Test
    @Order(2)
    void configsApi() {
        Map result = this.retrieve(HttpRequest.GET(CONNECT_URL + "/configs"), Map.class);
        assertEquals(PATH1, result.get("file"));
    }

    @Test
    @Order(2)
    void pluginsListApi() {
        List<ConnectPlugin> result = this.retrieveList(HttpRequest.GET(BASE_URL + "/plugins"), ConnectPlugin.class);
        assertEquals(2, result.size());
    }

    @Test
    @Order(2)
    void pluginsApi() {
        ConnectPlugin result = this.retrieve(HttpRequest.GET(BASE_URL + "/plugins/org.apache.kafka.connect.file.FileStreamSinkConnector"), ConnectPlugin.class);
        assertEquals("sink", result.getType());
        assertTrue(result.getDefinitions().size() > 0);
    }

    @Test
    @Order(5)
    void updateApi() {
        ConnectDefinition result = this.retrieve(HttpRequest.POST(
            CONNECT_URL + "/configs",
            ImmutableMap.of(
                "configs",              ImmutableMap.of(
                    "connector.class", "FileStreamSinkConnector",
                    "file", PATH2,
                    "topics", KafkaTestCluster.TOPIC_CONNECT
                )
            )
        ), ConnectDefinition.class);
        assertEquals(PATH2, result.getConfigs().get("file"));
    }

    @Test
    @Order(6)
    void definitionRestartApi() {
        this.exchange(HttpRequest.GET(CONNECT_URL + "/restart"));
    }

    @Test
    @Order(7)
    void definitionPauseApi() {
        this.exchange(HttpRequest.GET(CONNECT_URL + "/pause"));
    }

    @Test
    @Order(8)
    void definitionResumeApi() {
        this.exchange(HttpRequest.GET(CONNECT_URL + "/resume"));
    }

    @Test
    @Order(9)
    void taskRestartApi() {
        this.exchange(HttpRequest.GET(CONNECT_URL + "/tasks/0/restart"));

    }

    @Test
    @Order(10)
    void deleteApi() {
        this.exchange(
            HttpRequest.DELETE(CONNECT_URL)
        );

        this.exchange(
            HttpRequest.DELETE("/api/" + KafkaTestCluster.CLUSTER_ID + "/group/connect-connect-controller-test")
        );
    }
}
