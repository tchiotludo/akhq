package org.akhq.controllers;

import com.google.common.collect.ImmutableMap;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.views.View;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import org.akhq.utils.ResultPagedList;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.akhq.configs.Role;
import org.akhq.models.Schema;
import org.akhq.modules.RequestHelper;
import org.akhq.repositories.SchemaRegistryRepository;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Secured(Role.ROLE_REGISTRY_READ)
@Controller("${akhq.server.base-path:}/")
public class SchemaController extends AbstractController {
    private final SchemaRegistryRepository schemaRepository;

    @Value("${akhq.pagination.page-size}")
    private Integer pageSize;

    @Inject
    public SchemaController(SchemaRegistryRepository schemaRepository) {
        this.schemaRepository = schemaRepository;
    }

    @View("schemaList")
    @Get("{cluster}/schema")
    @Hidden
    public HttpResponse<?> list(
        HttpRequest<?> request,
        String cluster,
        Optional<String> search,
        Optional<Integer> page
    ) throws IOException, RestClientException, ExecutionException, InterruptedException {

        URIBuilder uri = URIBuilder.fromURI(request.getUri());
        Pagination pagination = new Pagination(pageSize, uri, page.orElse(1));

        PagedList<Schema> list = this.schemaRepository.list(cluster, pagination, search);

        return this.template(
            request,
            cluster,
            "schemas", list,
            "search", search,
            "pagination", ImmutableMap.builder()
                .put("size", list.total())
                .put("before", list.before().toNormalizedURI(false).toString())
                .put("after", list.after().toNormalizedURI(false).toString())
                .build()
        );
    }

    @Get("api/{cluster}/schema")
    @Operation(tags = {"schema registry"}, summary = "List all schemas")
    public ResultPagedList<Schema> listApi(
        HttpRequest<?> request,
        String cluster,
        Optional<String> search,
        Optional<Integer> page
    ) throws IOException, RestClientException, ExecutionException, InterruptedException {

        URIBuilder uri = URIBuilder.fromURI(request.getUri());
        Pagination pagination = new Pagination(pageSize, uri, page.orElse(1));

        return ResultPagedList.of(this.schemaRepository.list(cluster, pagination, search));
    }

    @Get("{cluster}/schema/id/{id}")
    @Hidden
    public HttpResponse<?> redirectId(
        HttpRequest<?> request,
        String cluster,
        Integer id
    ) throws IOException, RestClientException, URISyntaxException, ExecutionException, InterruptedException {
        Schema find = this.schemaRepository.getById(cluster, id);

        if (find != null) {
            return HttpResponse.redirect(this.uri("/" + cluster + "/schema/" + find.getSubject() + "/version#" + id));
        } else {
            MutableHttpResponse<Void> response = HttpResponse.redirect(this.uri("/" + cluster + "/schema"));

            this.toast(response, AbstractController.Toast.builder()
                .message("Unable to find avro schema for id '" + id + "'")
                .type(AbstractController.Toast.Type.error)
                .build()
            );

            return response;
        }
    }

    @Secured(Role.ROLE_REGISTRY_INSERT)
    @View("schemaCreate")
    @Get("{cluster}/schema/create")
    @Hidden
    public HttpResponse<?> create(HttpRequest<?> request, String cluster) throws IOException, RestClientException {
        return this.template(
            request,
            cluster,
            "config", this.schemaRepository.getDefaultConfig(cluster),
            "compatibilityLevel", Schema.Config.getCompatibilityLevelConfigList()
        );
    }

    @Secured(Role.ROLE_REGISTRY_INSERT)
    @Post(value = "{cluster}/schema/create", consumes = MediaType.MULTIPART_FORM_DATA)
    @Hidden
    public HttpResponse<?> createSubmit(
        String cluster,
        String subject,
        String schema,
        String compatibilityLevel
    ) throws Throwable {
        if (this.schemaRepository.exist(cluster, subject)) {
            MutableHttpResponse<Void> response = HttpResponse.redirect(this.uri("/" + cluster + "/schema/create"));

            this.toast(response, AbstractController.Toast.builder()
                .message("Subject '" + subject + "' already exits")
                .type(AbstractController.Toast.Type.error)
                .build()
            );

            return response;
        }

        MutableHttpResponse<Void> response = HttpResponse.ok();

        Toast toast = this.toast(response, RequestHelper.runnableToToast(
            () -> registerSchema(cluster, subject, schema, compatibilityLevel),
            "Schema '" + subject + "' is created",
            "Failed to create schema'" + subject + "'"
        ));

        URI redirect;

        if (toast.getType() != Toast.Type.error) {
            redirect = this.uri("/" + cluster + "/schema/" + subject);
        } else {
            redirect = this.uri("/" + cluster + "/schema/create");
        }

        return response.status(HttpStatus.MOVED_PERMANENTLY)
            .headers((headers) ->
                headers.location(redirect)
            );
    }

    @Secured(Role.ROLE_REGISTRY_INSERT)
    @Post(value = "api/{cluster}/schema")
    @Operation(tags = {"schema registry"}, summary = "Create a new schema")
    public Schema createApi(
        String cluster,
        @Body Schema schema
    ) throws Throwable {
        if (this.schemaRepository.exist(cluster, schema.getSubject())) {
            throw new IllegalArgumentException("Subject '" + schema.getSubject() + "' already exits");
        }

        return registerSchema(cluster, schema);
    }

    @View("schema")
    @Get("{cluster}/schema/{subject}")
    @Hidden
    public HttpResponse<?> home(HttpRequest<?> request, String cluster, String subject) throws IOException, RestClientException {
        return this.render(request, cluster, subject, "update");
    }

    @Get("api/{cluster}/schema/{subject}")
    @Operation(tags = {"schema registry"}, summary = "Retrieve a schema")
    public Schema homeApi(HttpRequest<?> request, String cluster, String subject) throws IOException, RestClientException {
        return this.schemaRepository.getLatestVersion(cluster, subject);
    }

    @Secured(Role.ROLE_REGISTRY_UPDATE)
    @Post(value = "{cluster}/schema/{subject}", consumes = MediaType.MULTIPART_FORM_DATA)
    @Hidden
    public HttpResponse<?> updateSchema(
        String cluster,
        String subject,
        String schema,
        String compatibilityLevel
    ) throws Throwable {
        MutableHttpResponse<Void> response = HttpResponse.redirect(this.uri("/" + cluster + "/schema/" + subject));

        this.toast(response, RequestHelper.runnableToToast(
            () -> registerSchema(cluster, subject, schema, compatibilityLevel),
            "Schema '" + subject + "' is updated",
            "Failed to update schema '" + subject + "'"
        ));

        return response;
    }

    @Secured(Role.ROLE_REGISTRY_UPDATE)
    @Post(value = "api/{cluster}/schema/{subject}")
    @Operation(tags = {"schema registry"}, summary = "Update a schema")
    public Schema updateSchemaApi(String cluster, String subject, @Body Schema schema) throws Throwable {
        if (!this.schemaRepository.exist(cluster, subject)) {
            throw new IllegalArgumentException("Subject '" + subject + "' doesn't exits");
        }

        if (!subject.equals(schema.getSubject())) {
            throw new IllegalArgumentException("Invalid subject name '" + subject + "', doesn't egals '" + schema.getSubject() + "'");
        }

        return registerSchema(cluster, schema);
    }

    private Schema registerSchema(String cluster, @Body Schema schema) throws IOException, RestClientException {
        Schema register = this.schemaRepository.register(cluster, schema.getSubject(), schema.getSchema());

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

    @View("schema")
    @Get("{cluster}/schema/{subject}/{tab:(version)}")
    @Hidden
    public HttpResponse<?> tab(HttpRequest<?> request, String cluster, String subject, String tab) throws IOException, RestClientException {
        return this.render(request, cluster, subject, tab);
    }

    @Get("api/{cluster}/schema/{subject}/version")
    @Operation(tags = {"schema registry"}, summary = "List all version for a schema")
    public List<Schema> versionsApi(HttpRequest<?> request, String cluster, String subject) throws IOException, RestClientException {
        return this.schemaRepository.getAllVersions(cluster, subject);
    }

    @Secured(Role.ROLE_REGISTRY_DELETE)
    @Get("{cluster}/schema/{subject}/delete")
    @Hidden
    public HttpResponse<?> delete(String cluster, String subject) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
            this.schemaRepository.delete(cluster, subject),
            "Subject from '" + subject + "' is deleted",
            "Failed to delete subject from '" + subject + "'"
        ));

        return response;
    }

    @Secured(Role.ROLE_REGISTRY_DELETE)
    @Delete("api/{cluster}/schema/{subject}")
    @Operation(tags = {"schema registry"}, summary = "Delete a schema")
    public HttpResponse<?> deleteApi(String cluster, String subject) throws IOException, RestClientException {
        if (!this.schemaRepository.exist(cluster, subject)) {
            throw new IllegalArgumentException("Subject '" + subject + "' doesn't exits");
        }

        this.schemaRepository.delete(cluster, subject);

        return HttpResponse.noContent();
    }

    @Secured(Role.ROLE_REGISTRY_VERSION_DELETE)
    @Get("{cluster}/schema/{subject}/version/{version}/delete")
    @Hidden
    public HttpResponse<?> deleteVersion(HttpRequest<?> request, String cluster, String subject, Integer version) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
                this.schemaRepository.deleteVersion(cluster, subject, version),
            "Version '" + version + "' from '" + subject + "' is deleted",
            "Failed to delete version '" + version + "' from '" + subject + "'"
        ));

        return response;
    }

    @Secured(Role.ROLE_REGISTRY_VERSION_DELETE)
    @Delete("api/{cluster}/schema/{subject}/version/{version}")
    @Operation(tags = {"schema registry"}, summary = "Delete a version for a schema")
    public HttpResponse<?> deleteVersionApi(
        String cluster,
        String subject,
        Integer version
    ) throws IOException, RestClientException {
        this.schemaRepository.deleteVersion(cluster, subject, version);

        return HttpResponse.noContent();
    }

    private Schema registerSchema(
        String cluster,
        String subject,
        String schema,
        String compatibilityLevel
    ) throws IOException, RestClientException {
        org.apache.avro.Schema avroSchema = new org.apache.avro.Schema.Parser().parse(schema);

        Schema register = this.schemaRepository.register(cluster, subject, avroSchema);

        Schema.Config config = Schema.Config.builder()
            .compatibilityLevel(Schema.Config.CompatibilityLevelConfig.valueOf(
                compatibilityLevel
            ))
            .build();
        this.schemaRepository.updateConfig(cluster, subject, config);

        return register;
    }

    private HttpResponse<?> render(HttpRequest<?> request, String cluster, String subject, String tab) throws IOException, RestClientException {
        return this.template(
            request,
            cluster,
            "tab", tab,
            "schema", this.schemaRepository.getLatestVersion(cluster, subject),
            "versions", this.schemaRepository.getAllVersions(cluster, subject),
            "config", this.schemaRepository.getConfig(cluster, subject),
            "compatibilityLevel", Schema.Config.getCompatibilityLevelConfigList()
        );
    }
}
