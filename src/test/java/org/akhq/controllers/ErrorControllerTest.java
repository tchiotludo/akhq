package org.akhq.controllers;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(environments = "test")
@Property(name = "micronaut.http.client.follow-redirects", value = "false")
@Property(name = "akhq.connections.local.properties.bootstrap.servers", value = "localhost:9092")
class ErrorControllerTest {
    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void rejectsAnonymousMcpRequestsWithUnauthorized() {
        assertEquals(HttpStatus.UNAUTHORIZED, status(mcpRequest()));
    }

    @Test
    void rejectsMcpRequestsWithAnInvalidTokenWithUnauthorized() {
        assertEquals(HttpStatus.UNAUTHORIZED, status(mcpRequest().bearerAuth("invalid.token.value")));
    }

    private MutableHttpRequest<String> mcpRequest() {
        return HttpRequest.POST("/mcp", "");
    }

    private HttpStatus status(MutableHttpRequest<String> request) {
        try {
            return client.toBlocking().exchange(request).getStatus();
        } catch (HttpClientResponseException e) {
            return e.getStatus();
        }
    }
}
