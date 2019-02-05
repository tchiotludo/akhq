package org.kafkahq.controllers;

import com.google.inject.Inject;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.jooby.*;
import org.jooby.mvc.GET;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.kafkahq.models.Schema;
import org.kafkahq.modules.RequestHelper;
import org.kafkahq.repositories.SchemaRegistryRepository;

import java.io.IOException;

@Path("/{cluster}/schema")
public class SchemaController extends AbstractController {
    @Inject
    private SchemaRegistryRepository schemaRepository;

    @GET
    public View list(Request request, String cluster) throws IOException, RestClientException {
        return this.template(
            request,
            cluster,
            Results
                .html("schemaList")
                .put("schemas", this.schemaRepository.getAll(cluster))
        );
    }

    @GET
    @Path("create")
    public View create(Request request, String cluster) throws IOException, RestClientException {
        return this.template(
            request,
            cluster,
            Results
                .html("schemaCreate")
                .put("config", this.schemaRepository.getDefaultConfig(cluster))
                .put("compatibilityLevel", Schema.Config.getCompatibilityLevelConfigList())
        );
    }

    @POST
    @Path("create")
    public Result createSubmit(Request request, Response response, String cluster, String subject) throws Throwable {
        if (this.schemaRepository.exist(cluster, subject)) {
            this.toast(request, AbstractController.Toast.builder()
                .message("Subject '" + subject + "' already exits")
                .type(AbstractController.Toast.Type.error)
                .build()
            );
            response.redirect("/" + cluster + "/schema/create");
            return Results.ok();
        }

        Toast toast = this.toast(request, RequestHelper.runnableToToast(
            () -> registerSchema(request, cluster, subject),
            "Schema '" + subject + "' is created",
            "Failed to create schema'" + subject + "'"
        ));

        if (toast.getType() != Toast.Type.error) {
            response.redirect("/" + cluster + "/schema/" + subject);
        } else {
            response.redirect("/" + cluster + "/schema/create");
        }

        return Results.ok();
    }

    @GET
    @Path("{subject}")
    public View home(Request request, String cluster, String subject) throws IOException, RestClientException {
        return this.render(request, cluster, subject, "update");
    }

    @POST
    @Path("{subject}")
    public Result updateSchema(Request request, Response response, String cluster, String subject) throws Throwable {
        Toast toast = this.toast(request, RequestHelper.runnableToToast(
            () -> registerSchema(request, cluster, subject),
            "Schema '" + subject + "' is updated",
            "Failed to update schema'" + subject + "'"
        ));

        response.redirect("/" + cluster + "/schema/" + subject);
        return Results.ok();
    }

    @GET
    @Path("{subject}/{tab:(version)}")
    public View tab(Request request, String cluster, String subject, String tab) throws IOException, RestClientException {
        return this.render(request, cluster, subject, tab);
    }

    @GET
    @Path("{subject}/delete")
    public Result delete(Request request, String cluster, String subject) {
        this.toast(request, RequestHelper.runnableToToast(() ->
                this.schemaRepository.delete(cluster, subject),
            "Subject from '" + subject + "' is deleted",
            "Failed to delete subject from '" + subject + "'"
        ));

        return Results.ok();
    }

    @GET
    @Path("{subject}/version/{version}/delete")
    public Result deleteVersion(Request request, String cluster, String subject, Integer version) {
        this.toast(request, RequestHelper.runnableToToast(() ->
                this.schemaRepository.deleteVersion(cluster, subject, version),
            "Version '" + version + "' from '" + subject + "' is deleted",
            "Failed to delete version '" + version + "' from '" + subject + "'"
        ));

        return Results.ok();
    }


    private Schema registerSchema(Request request, String cluster, String subject) throws IOException, RestClientException {
        org.apache.avro.Schema avroSchema = new org.apache.avro.Schema.Parser().parse(
            request.param("schema").value()
        );

        Schema register = this.schemaRepository.register(cluster, subject, avroSchema);

        Schema.Config config = Schema.Config.builder()
            .compatibilityLevel(Schema.Config.CompatibilityLevelConfig.valueOf(
                request.param("compatibility-level").value()
            ))
            .build();
        this.schemaRepository.updateConfig(cluster, subject, config);

        return register;
    }

    public View render(Request request, String cluster, String subject, String tab) throws IOException, RestClientException {
        return this.template(
            request,
            cluster,
            Results
                .html("schema")
                .put("tab", tab)
                .put("schema", this.schemaRepository.getLatestVersion(cluster, subject))
                .put("versions", this.schemaRepository.getAllVersions(cluster, subject))
                .put("config", this.schemaRepository.getConfig(cluster, subject))
                .put("compatibilityLevel", Schema.Config.getCompatibilityLevelConfigList())
        );
    }
}
