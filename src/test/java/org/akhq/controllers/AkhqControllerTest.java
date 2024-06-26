package org.akhq.controllers;

import io.micronaut.http.HttpRequest;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(1, result.get(0).getKsqldbs().size());
        assertEquals("ksqldb", result.get(0).getKsqldbs().get(0));
        assertTrue(result.get(0).isRegistry());
        assertEquals("CONFLUENT", result.get(0).getRegistryType());
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
        assertEquals(3, result.getRoles().size());
        assertThat(result.getRoles().stream().map(AkhqController.AuthUser.AuthPermissions::getPatterns).flatMap(Collection::stream).collect(Collectors.toList()),
            containsInAnyOrder(".*", "user.*", "public.*"));
        assertThat(result.getRoles().stream().map(AkhqController.AuthUser.AuthPermissions::getClusters).flatMap(Collection::stream).collect(Collectors.toList()),
            containsInAnyOrder(".*", ".*", "pub.*"));
    }
}
