package org.akhq.controllers;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.client.sse.RxSseClient;
import io.micronaut.runtime.server.EmbeddedServer;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.Record;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Property(name = "micronaut.security.enabled", value = "false")
class SseControllerTest extends AbstractTest {
    public static final String BASE_URL = "/api/" + KafkaTestCluster.CLUSTER_ID + "/topic";

    @Inject
    private EmbeddedServer embeddedServer;

    @Test
    public void searchApi() {
        RxSseClient sseClient = embeddedServer.getApplicationContext().createBean(RxSseClient.class, embeddedServer.getURL());

        List<Record> results = sseClient
            .eventStream(BASE_URL + "/" + KafkaTestCluster.TOPIC_HUGE + "/data/search?searchByKey=key_100_C", TopicController.SearchRecord.class)
            .toList()
            .blockingGet()
            .stream()
            .flatMap(r -> r.getData() != null && r.getData().getRecords() != null ? r.getData().getRecords().stream() : Stream.empty())
            .collect(Collectors.toList());

        assertThat(results.size(), is(3));
    }
}
