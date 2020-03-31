package org.akhq.controllers;

import com.google.common.collect.ImmutableMap;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.*;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.views.View;
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
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Secured(Role.ROLE_REGISTRY_READ)
@Controller("${akhq.server.base-path:}/{cluster}/schema")
public class SchemaController extends AbstractController {
    private SchemaRegistryRepository schemaRepository;

    @Value("${akhq.pagination.page-size}")
    private Integer pageSize;

    @Inject
    public SchemaController(SchemaRegistryRepository schemaRepository) {
        this.schemaRepository = schemaRepository;
    }

    @View("schemaList")
    @Get
    public HttpResponse list(
        HttpRequest request,
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

    @Get("id/{id}")
    public HttpResponse redirectId(HttpRequest request, String cluster, Integer id) throws IOException, RestClientException, URISyntaxException, ExecutionException, InterruptedException {
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
    @Get("create")
    public HttpResponse create(HttpRequest request, String cluster) throws IOException, RestClientException {
        return this.template(
            request,
            cluster,
            "config", this.schemaRepository.getDefaultConfig(cluster),
            "compatibilityLevel", Schema.Config.getCompatibilityLevelConfigList()
        );
    }

    @Secured(Role.ROLE_REGISTRY_INSERT)
    @Post(value = "create", consumes = MediaType.MULTIPART_FORM_DATA)
    public HttpResponse createSubmit(String cluster,
                                     String subject,
                                     String schema,
                                     String compatibilityLevel)
        throws Throwable
    {
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

    @View("schema")
    @Get("{subject}")
    public HttpResponse home(HttpRequest request, String cluster, String subject) throws IOException, RestClientException {
        return this.render(request, cluster, subject, "update");
    }

    @Secured(Role.ROLE_REGISTRY_UPDATE)
    @Post(value = "{subject}", consumes = MediaType.MULTIPART_FORM_DATA)
    public HttpResponse updateSchema(String cluster,
                                     String subject,
                                     String schema,
                                     String compatibilityLevel) throws Throwable {
        MutableHttpResponse<Void> response = HttpResponse.redirect(this.uri("/" + cluster + "/schema/" + subject));

        this.toast(response, RequestHelper.runnableToToast(
            () -> registerSchema(cluster, subject, schema, compatibilityLevel),
            "Schema '" + subject + "' is updated",
            "Failed to update schema '" + subject + "'"
        ));

        return response;
    }

    @View("schema")
    @Get("{subject}/{tab:(version)}")
    public HttpResponse tab(HttpRequest request, String cluster, String subject, String tab) throws IOException, RestClientException {
        return this.render(request, cluster, subject, tab);
    }

    @Secured(Role.ROLE_REGISTRY_DELETE)
    @Get("{subject}/delete")
    public HttpResponse delete(String cluster, String subject) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
            this.schemaRepository.delete(cluster, subject),
            "Subject from '" + subject + "' is deleted",
            "Failed to delete subject from '" + subject + "'"
        ));

        return response;
    }

    @Secured(Role.ROLE_REGISTRY_VERSION_DELETE)
    @Get("{subject}/version/{version}/delete")
    public HttpResponse deleteVersion(HttpRequest request, String cluster, String subject, Integer version) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
                this.schemaRepository.deleteVersion(cluster, subject, version),
            "Version '" + version + "' from '" + subject + "' is deleted",
            "Failed to delete version '" + version + "' from '" + subject + "'"
        ));

        return response;
    }

    private Schema registerSchema(String cluster, String subject, String schema, String compatibilityLevel) throws IOException, RestClientException {
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

    private HttpResponse render(HttpRequest request, String cluster, String subject, String tab) throws IOException, RestClientException {
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
