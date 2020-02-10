package org.kafkahq.rest;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import org.kafkahq.models.Cluster;

import java.util.List;

@Controller("${kafkahq.server.base-path:}/api")
public class ClusterResource {

    @Get("/clusters")
    public List<Cluster> fetchAllClusters() {
        return null;
    }
}
