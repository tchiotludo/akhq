package org.akhq.controllers;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.swagger.v3.oas.annotations.Operation;
import org.akhq.configs.Role;
import org.akhq.middlewares.SchemaComparator;
import org.akhq.models.Schema;
import org.akhq.models.TopicSchema;
import org.akhq.repositories.SchemaRegistryRepository;
import org.akhq.utils.Pagination;
import org.akhq.utils.ResultPagedList;
import org.codehaus.httpcache4j.uri.URIBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.inject.Inject;

@Secured(Role.ROLE_REGISTRY_READ)
@Controller
public class SchemaController extends AbstractController {
    private final SchemaRegistryRepository schemaRepository;

    @Value("${akhq.pagination.page-size}")
    private Integer pageSize;

    @Inject
    public SchemaController(SchemaRegistryRepository schemaRepository) {
        this.schemaRepository = schemaRepository;
    }

    @Get("api/{cluster}/schema")
    @Operation(tags = {"schema registry"}, summary = "List all schemas")
    public ResultPagedList<Schema> list(
        HttpRequest<?> request,
        String cluster,
        Optional<String> search,
        Optional<Integer> page
    ) throws IOException, RestClientException, ExecutionException, InterruptedException {

        URIBuilder uri = URIBuilder.fromURI(request.getUri());
        Pagination pagination = new Pagination(pageSize, uri, page.orElse(1));

        return ResultPagedList.of(this.schemaRepository.list(cluster, pagination, search));
    }

    @Get("api/{cluster}/schema/topic/{topic}")
    @Operation(tags = {"schema registry"}, summary = "List all schemas prefered schemas for this topic")
    public TopicSchema listSchemaForTopic(
        HttpRequest<?> request,
        String cluster,
        String topic
    ) throws IOException, RestClientException {
        List<Schema> schemas = this.schemaRepository.listAll(cluster, Optional.empty());

        return new TopicSchema(
            schemas.stream()
                .sorted(new SchemaComparator(topic, true))
                .collect(Collectors.toList()),
            schemas.stream()
                .sorted(new SchemaComparator(topic, false))
                .collect(Collectors.toList())
        );
    }

    @Secured(Role.ROLE_REGISTRY_INSERT)
    @Post(value = "api/{cluster}/schema")
    @Operation(tags = {"schema registry"}, summary = "Create a new schema")
    public Schema create(
        String cluster,
        @Body Schema schema
    ) throws Throwable {
        if (this.schemaRepository.exist(cluster, schema.getSubject())) {
            throw new IllegalArgumentException("Subject '" + schema.getSubject() + "' already exits");
        }

        return registerSchema(cluster, schema);
    }

    @Get("api/{cluster}/schema/{subject}")
    @Operation(tags = {"schema registry"}, summary = "Retrieve a schema")
    public Schema home(HttpRequest<?> request, String cluster, String subject) throws IOException, RestClientException {
        return this.schemaRepository.getLatestVersion(cluster, subject);
    }

    @Secured(Role.ROLE_REGISTRY_UPDATE)
    @Post(value = "api/{cluster}/schema/{subject}")
    @Operation(tags = {"schema registry"}, summary = "Update a schema")
    public Schema updateSchema(String cluster, String subject, @Body Schema schema) throws Throwable {
        if (!this.schemaRepository.exist(cluster, subject)) {
            throw new IllegalArgumentException("Subject '" + subject + "' doesn't exits");
        }

        if (!subject.equals(schema.getSubject())) {
            throw new IllegalArgumentException("Invalid subject name '" + subject + "', doesn't egals '" + schema.getSubject() + "'");
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
        return this.schemaRepository.getById(cluster, id);
    }

    @Get("api/{cluster}/schema/{subject}/version")
    @Operation(tags = {"schema registry"}, summary = "List all version for a schema")
    public List<Schema> versions(HttpRequest<?> request, String cluster, String subject) throws IOException, RestClientException {
        return this.schemaRepository.getAllVersions(cluster, subject);
    }

    @Secured(Role.ROLE_REGISTRY_DELETE)
    @Delete("api/{cluster}/schema/{subject}")
    @Operation(tags = {"schema registry"}, summary = "Delete a schema")
    public HttpResponse<?> delete(String cluster, String subject) throws IOException, RestClientException {
        if (!this.schemaRepository.exist(cluster, subject)) {
            throw new IllegalArgumentException("Subject '" + subject + "' doesn't exits");
        }

        this.schemaRepository.delete(cluster, subject);

        return HttpResponse.noContent();
    }

    @Secured(Role.ROLE_REGISTRY_VERSION_DELETE)
    @Delete("api/{cluster}/schema/{subject}/version/{version}")
    @Operation(tags = {"schema registry"}, summary = "Delete a version for a schema")
    public HttpResponse<?> deleteVersion(
        String cluster,
        String subject,
        Integer version
    ) throws IOException, RestClientException {
        this.schemaRepository.deleteVersion(cluster, subject, version);

        return HttpResponse.noContent();
    }
}
