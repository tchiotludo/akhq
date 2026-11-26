package org.akhq.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Inject;
import io.micronaut.context.annotation.Value;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class SchemDropDownController {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @Client("${schema.registry.url}")
    HttpClient schemaRegistryClient;

    @Post("/add-schema")
    public HttpResponse<Map<String, Object>> createSchema(@Body Map<String, Object> req) {
        try {
            String subject = (String) req.get("subject");
            Object schemaObj = req.get("schema");
            String schemaString = objectMapper.writeValueAsString(schemaObj);

            Map<String, Object> body = Map.of("schema", schemaString);

            // call Schema Registry
            Object response = schemaRegistryClient.toBlocking().retrieve(
                    HttpRequest.POST(
                            "/subjects/" + subject + "/versions",
                            body));

            return HttpResponse.ok(
                    Map.of("schemId", ((Map<String, Object>) response).get("id")));
        } catch (Exception e) {
            return HttpResponse.serverError(
                    Map.of("error", e.getMessage()));
        }

    }

}
