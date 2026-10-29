package org.akhq.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    private static final String KEY_100_EXPRESSION = """
        {"version":1,"groups":[{"conditions":[{"field":"key","operator":"contains","value":"key_100"}]}]}
        """;

    @Test
    void searchApiWithExpression() {
        SseClient sseClient = embeddedServer.getApplicationContext().createBean(SseClient.class, embeddedServer.getURL());

        HttpRequest<?> request = HttpRequest.POST(URI.create(BASE_URL + "/" + KafkaTestCluster.TOPIC_HUGE + "/data/search"), KEY_100_EXPRESSION)
            .contentType(MediaType.APPLICATION_JSON_TYPE)
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

    @Test
    void searchApiWithEmptyExpressionDoesNotFilter() {
        SseClient sseClient = embeddedServer.getApplicationContext().createBean(SseClient.class, embeddedServer.getURL());

        HttpRequest<?> request = HttpRequest.POST(URI.create(BASE_URL + "/" + KafkaTestCluster.TOPIC_HUGE + "/data/search"), "{\"version\":1,\"groups\":[]}")
            .contentType(MediaType.APPLICATION_JSON_TYPE)
            .basicAuth("admin", "pass");

        List<Record> records = Flux.from(sseClient.eventStream(request, TopicController.SearchRecord.class))
            .collectList()
            .block()
            .stream()
            .map(Event::getData)
            .flatMap(r -> r != null && r.getRecords() != null ? r.getRecords().stream() : Stream.empty())
            .collect(Collectors.toList());

        assertThat(records.size(), is(greaterThan(3)));
    }

    @Test
    void searchApiWithRegexExpression() {
        SseClient sseClient = embeddedServer.getApplicationContext().createBean(SseClient.class, embeddedServer.getURL());
        String expression = """
            {"version":1,"groups":[{"conditions":[{"field":"key","operator":"matchesRegex","value":"^key_10[01]$"}]}]}
            """;

        HttpRequest<?> request = HttpRequest.POST(URI.create(BASE_URL + "/" + KafkaTestCluster.TOPIC_HUGE + "/data/search"), expression)
            .contentType(MediaType.APPLICATION_JSON_TYPE)
            .basicAuth("admin", "pass");

        List<Record> records = Flux.from(sseClient.eventStream(request, TopicController.SearchRecord.class))
            .collectList()
            .block()
            .stream()
            .map(Event::getData)
            .flatMap(r -> r != null && r.getRecords() != null ? r.getRecords().stream() : Stream.empty())
            .collect(Collectors.toList());

        assertThat(records.size() > 0, is(true));
        assertThat(records.stream().allMatch(r -> r.getKey().matches("^key_10[01]$")), is(true));
    }

    @Test
    void searchApiWithInvalidExpressionReturnsBadRequest() {
        String invalidRegex = """
            {"version":1,"groups":[{"conditions":[{"field":"value","operator":"matchesRegex","value":"(unclosed"}]}]}
            """;

        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> client.toBlocking().exchange(
            HttpRequest.POST(BASE_URL + "/" + KafkaTestCluster.TOPIC_HUGE + "/data/search", invalidRegex)
                .contentType(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.TEXT_EVENT_STREAM_TYPE)
                .basicAuth("admin", "pass")
        ));

        assertThat(e.getStatus(), is(HttpStatus.BAD_REQUEST));
        assertThat(e.getMessage().contains("Invalid regular expression"), is(true));
    }

    @Test
    void searchApiWithUnknownOperatorReturnsBadRequest() {
        String unknownOperator = """
            {"version":1,"groups":[{"conditions":[{"field":"value","operator":"startsWith","value":"x"}]}]}
            """;

        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> client.toBlocking().exchange(
            HttpRequest.POST(BASE_URL + "/" + KafkaTestCluster.TOPIC_HUGE + "/data/search", unknownOperator)
                .contentType(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.TEXT_EVENT_STREAM_TYPE)
                .basicAuth("admin", "pass")
        ));

        assertThat(e.getStatus(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    void downloadApiWithExpression() throws Exception {
        String body = client.toBlocking().retrieve(
            HttpRequest.POST(BASE_URL + "/" + KafkaTestCluster.TOPIC_HUGE + "/data/download", KEY_100_EXPRESSION)
                .contentType(MediaType.APPLICATION_JSON_TYPE)
                .basicAuth("admin", "pass")
        );

        assertThat(new ObjectMapper().readTree(body).size(), is(3));
    }
}
