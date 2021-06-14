package org.akhq.controllers;

import io.micronaut.http.HttpRequest;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AkhqControllerTest extends AbstractTest {
    @Test
    void list() {
        List<AkhqController.ClusterDefinition> result = this.retrieveList(
            HttpRequest.GET("/api/cluster"),
            AkhqController.ClusterDefinition.class
        );

        assertEquals(1, result.size());
        assertEquals(KafkaTestCluster.CLUSTER_ID, result.get(0).getId());
        assertEquals(2, result.get(0).getConnects().size());
        assertEquals("connect-1", result.get(0).getConnects().get(0));
        assertTrue(result.get(0).isRegistry());
    }

    @Test
    void auth() {
        AkhqController.AuthDefinition result = this.retrieve(
            HttpRequest.GET("/api/auths"),
            AkhqController.AuthDefinition.class
        );

        assertTrue(result.isLoginEnabled());
        assertTrue(result.isFormEnabled());
    }

    @Test
    void user() {
        AkhqController.AuthUser result = this.retrieve(
            HttpRequest.GET("/api/me"),
            AkhqController.AuthUser.class
        );

        assertEquals("admin", result.getUsername());
        assertEquals(35, result.getRoles().size());
    }
}
