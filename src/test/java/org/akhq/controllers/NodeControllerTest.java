package org.akhq.controllers;

import com.google.common.collect.ImmutableMap;
import io.micronaut.http.HttpRequest;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.Cluster;
import org.akhq.models.Config;
import org.akhq.models.LogDir;
import org.akhq.models.Node;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeControllerTest extends AbstractTest {
    @Test
    void listApi() {
        Cluster result = this.retrieve(
            HttpRequest.GET("/api/" +  KafkaTestCluster.CLUSTER_ID + "/node"),
            Cluster.class
        );

        assertEquals(1, result.getNodes().size());
    }

    @Test
    void nodeApi() {
        Node result = this.retrieve(
            HttpRequest.GET("/api/" +  KafkaTestCluster.CLUSTER_ID + "/node/0"),
            Node.class
        );

        assertEquals(0, result.getId());
    }

    @Test
    void nodeConfigApi() {
        List<Config> result = this.retrieveList(
            HttpRequest.GET("/api/" +  KafkaTestCluster.CLUSTER_ID + "/node/0/configs"),
            Config.class
        );

        assertEquals("2", result.stream().filter(config -> config.getName().equals("num.io.threads")).findFirst().orElseThrow().getValue());
    }

    @Test
    void nodeConfigUpdateApi() {
        String s = String.valueOf(new Random().nextInt((Integer.MAX_VALUE - Integer.MAX_VALUE/2) + 1) + Integer.MAX_VALUE/2);

        List<Config> result = this.retrieveList(
            HttpRequest.POST(
                "/api/" +  KafkaTestCluster.CLUSTER_ID + "/node/0/configs",
                ImmutableMap.of("max.connections.per.ip", s)
            ),
            Config.class
        );

        assertEquals(s, result.stream().filter(config -> config.getName().equals("max.connections.per.ip")).findFirst().orElseThrow().getValue());
    }

    @Test
    void nodeLogApi() {
        List<LogDir> result = this.retrieveList(
            HttpRequest.GET("/api/" +  KafkaTestCluster.CLUSTER_ID + "/node/0/logs"),
            LogDir.class
        );


        assertEquals(3, result.stream().filter(r -> r.getTopic().equals("stream-count")).count());
    }
}
