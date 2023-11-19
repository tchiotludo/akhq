package org.akhq.controllers;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import org.akhq.configs.security.Role;
import org.akhq.middlewares.SchemaComparator;
import org.akhq.models.Schema;
import org.akhq.models.TopicSchema;
import org.akhq.repositories.SchemaRegistryRepository;
import org.akhq.security.annotation.AKHQSecured;
import org.akhq.utils.Pagination;
import org.akhq.utils.ResultPagedList;
import org.codehaus.httpcache4j.uri.URIBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

@AKHQSecured(resource = Role.Resource.SCHEMA, action = Role.Action.READ)
@Controller
public class SchemaController extends AbstractController {
    private final SchemaRegistryRepository schemaRepository;

    private String decode(String value) {
        try {
        return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Value("${akhq.pagination.page-size}")
    private Integer pageSize;

    @Inject
    public SchemaController(SchemaRegistryRepository schemaRepository) {
        this.schemaRepository = schemaRepository;
    }


    @Get("api/{cluster}/schemas")
    @Operation(tags = {"schema registry"}, summary = "List all schemas")
    public List<String> listAll(
        String cluster) throws RestClientException, IOException {
        return this.schemaRepository.all(cluster, Optional.empty(), List.of());
    }


    @Get("api/{cluster}/schema")
    @Operation(tags = {"schema registry"}, summary = "List all schemas")
    public ResultPagedList<Schema> list(
        HttpRequest<?> request,
        String cluster,
        Optional<String> search,
        Optional<Integer> page
    ) throws IOException, RestClientException, ExecutionException, InterruptedException {
        checkIfClusterAllowed(cluster);

        URIBuilder uri = URIBuilder.fromURI(request.getUri());
        Pagination pagination = new Pagination(pageSize, uri, page.orElse(1));

        return ResultPagedList.of(this.schemaRepository.list(
            cluster,
            pagination,
            search,
            buildUserBasedResourceFilters(cluster))
        );
    }

    @Get("api/{cluster}/schema/topic/{topic}")
    @Operation(tags = {"schema registry"}, summary = "List all schemas prefered schemas for this topic")
    public TopicSchema listSchemaForTopic(
        HttpRequest<?> request,
        String cluster,
        String topic
    ) throws IOException, RestClientException {
        checkIfClusterAndResourceAllowed(cluster, topic);

        List<Schema> schemas = this.schemaRepository.listAll(cluster, Optional.empty(), List.of());

        return new TopicSchema(
            schemas.stream()
                .sorted(new SchemaComparator(topic, true))
                .collect(Collectors.toList()),
            schemas.stream()
                .sorted(new SchemaComparator(topic, false))
                .collect(Collectors.toList())
        );
    }

    @AKHQSecured(resource = Role.Resource.SCHEMA, action = Role.Action.CREATE)
    @Post(value = "api/{cluster}/schema")
    @Operation(tags = {"schema registry"}, summary = "Create a new schema")
    public Schema create(
        String cluster,
        @Body Schema schema
    ) throws Throwable {
        checkIfClusterAllowed(cluster);

        if (this.schemaRepository.exist(cluster, schema.getSubject())) {
            throw new IllegalArgumentException("Subject '" + schema.getSubject() + "' already exists");
        }

        return registerSchema(cluster, schema);
    }

    @Get("api/{cluster}/schema/{subject}")
    @Operation(tags = {"schema registry"}, summary = "Retrieve a schema")
    public Schema home(HttpRequest<?> request, String cluster, String subject) throws IOException, RestClientException {
        checkIfClusterAndResourceAllowed(cluster, subject);

        return this.schemaRepository.getLatestVersion(cluster, decode(subject));
    }

    @AKHQSecured(resource = Role.Resource.SCHEMA, action = Role.Action.UPDATE)
    @Post(value = "api/{cluster}/schema/{subject}")
    @Operation(tags = {"schema registry"}, summary = "Update a schema")
    public Schema updateSchema(String cluster, String subject, @Body Schema schema) throws Throwable {
        checkIfClusterAndResourceAllowed(cluster, subject);

        final String decodedSubject = decode(subject);
        if (!this.schemaRepository.exist(cluster, decodedSubject)) {
            throw new IllegalArgumentException("Subject '" + decodedSubject + "' doesn't exist");
        }

        if (!decodedSubject.equals(schema.getSubject())) {
            throw new IllegalArgumentException("Invalid subject name '" + decodedSubject + "', doesn't match '" + schema.getSubject() + "'");
        }

        return registerSchema(cluster, schema);
    }

    private Schema registerSchema(String cluster, @Body Schema schema) throws IOException, RestClientException {
        Schema register = this.schemaRepository.register(cluster, schema.getSubject(), schema.getSchemaType(), schema.getSchema(), schema.getReferences());

        if (schema.getCompatibilityLevel() != null) {
            this.schemaRepository.updateConfig(
                cluster,
                schema.getSubject(),
                new Schema.Config(schema.getCompatibilityLevel())
            );

            register = new Schema(register, new Schema.Config(schema.getCompatibilityLevel()));
        }

        return register;
    }

    @Get("api/{cluster}/schema/id/{id}")
    @Operation(tags = {"schema registry"}, summary = "Find a schema by id")
    public Schema redirectId(
        HttpRequest<?> request,
        String cluster,
        Integer id
    ) throws IOException, RestClientException, ExecutionException, InterruptedException {
        // TODO Do the check on the subject name too
        checkIfClusterAllowed(cluster);

        return this.schemaRepository
            .getById(cluster, id)
            .orElse(null);
    }

    @Get("api/{cluster}/schema/{subject}/version")
    @Operation(tags = {"schema registry"}, summary = "List all version for a schema")
    public List<Schema> versions(HttpRequest<?> request, String cluster, String subject) throws IOException, RestClientException {
        checkIfClusterAndResourceAllowed(cluster, subject);

        return this.schemaRepository.getAllVersions(cluster, decode(subject));
    }

    @AKHQSecured(resource = Role.Resource.SCHEMA, action = Role.Action.DELETE)
    @Delete("api/{cluster}/schema/{subject}")
    @Operation(tags = {"schema registry"}, summary = "Delete a schema")
    public HttpResponse<?> delete(String cluster, String subject) throws IOException, RestClientException {
        checkIfClusterAndResourceAllowed(cluster, subject);

        final String decodedSubject = decode(subject);
        if (!this.schemaRepository.exist(cluster, decodedSubject)) {
            throw new IllegalArgumentException("Subject '" + decodedSubject + "' doesn't exist");
        }

        this.schemaRepository.delete(cluster, decodedSubject);

        return HttpResponse.noContent();
    }

    @AKHQSecured(resource = Role.Resource.SCHEMA, action = Role.Action.DELETE_VERSION)
    @Delete("api/{cluster}/schema/{subject}/version/{version}")
    @Operation(tags = {"schema registry"}, summary = "Delete a version for a schema")
    public HttpResponse<?> deleteVersion(
        String cluster,
        String subject,
        Integer version
    ) throws IOException, RestClientException {
        checkIfClusterAndResourceAllowed(cluster, subject);

        this.schemaRepository.deleteVersion(cluster, decode(subject), version);

        return HttpResponse.noContent();
    }
}
