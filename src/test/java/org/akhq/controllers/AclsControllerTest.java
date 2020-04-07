package org.akhq.controllers;

import io.micronaut.http.HttpRequest;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.AccessControl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AclsControllerTest extends AbstractTest {
    public static final String BASE_URL = "/api/" + KafkaTestCluster.CLUSTER_ID + "/acls";
    public static final String ACL_URL = BASE_URL + "/" + AccessControl.encodePrincipal("user:toto") ;

    @Test
    @Disabled("Break on github actions")
    void listApi() {
        List<AccessControl> result = this.retrieveList(HttpRequest.GET(BASE_URL), AccessControl.class);
        assertEquals(2, result.size());
    }

    @Test
    @Disabled("Break on github actions")
    void principalApi() {
        AccessControl result = this.retrieve(HttpRequest.GET(ACL_URL), AccessControl.class);
        assertEquals("user:toto", result.getPrincipal());
        assertEquals(5, result.getAcls().size());
    }
}
