package org.akhq.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.sse.SseClient;
import io.micronaut.http.sse.Event;
import io.micronaut.runtime.server.EmbeddedServer;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.Record;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class SseControllerTest extends AbstractTest {
    public static final String BASE_URL = "/api/" + KafkaTestCluster.CLUSTER_ID + "/topic";

    @Inject
    private EmbeddedServer embeddedServer;

    @Test
    void searchApi() {
        SseClient sseClient = embeddedServer.getApplicationContext().createBean(SseClient.class, embeddedServer.getURL());

        HttpRequest<?> request = HttpRequest.GET(URI.create(BASE_URL + "/" + KafkaTestCluster.TOPIC_HUGE + "/data/search?searchByKey=key_100_C"))
            .basicAuth("admin", "pass");

        List<Record> records = Flux.from(sseClient.eventStream(request, TopicController.SearchRecord.class))
            .collectList()
            .block()
            .stream()
            .map(Event::getData)
            .flatMap(r -> r != null && r.getRecords() != null ? r.getRecords().stream() : Stream.empty())
            .collect(Collectors.toList());

        assertThat(records.size(), is(3));
    }
}
