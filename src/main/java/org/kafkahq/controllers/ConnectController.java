package org.kafkahq.controllers;

import io.micronaut.http.*;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.views.View;
import org.kafkahq.configs.Role;
import org.kafkahq.models.ConnectDefinition;
import org.kafkahq.models.ConnectPlugin;
import org.kafkahq.modules.RequestHelper;
import org.kafkahq.repositories.ConnectRepository;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Secured(Role.ROLE_CONNECT_READ)
@Controller("${kafkahq.server.base-path:}/{cluster}/connect")
public class ConnectController extends AbstractController {
    private ConnectRepository connectRepository;

    @Inject
    public ConnectController(ConnectRepository connectRepository) {
        this.connectRepository = connectRepository;
    }

    @View("connectList")
    @Get
    public HttpResponse list(HttpRequest request, String cluster) {
        return this.template(
            request,
            cluster,
            "connects", this.connectRepository.getDefinitions(cluster)
        );
    }

    @Secured(Role.ROLE_CONNECT_INSERT)
    @View("connectCreate")
    @Get("create")
    public HttpResponse create(HttpRequest request, String cluster, Optional<String> type) {
        List<ConnectPlugin> plugins = this.connectRepository.getPlugins(cluster);

        return this.template(
            request,
            cluster,
            "plugins", plugins,
            "pluginDefinition", type
                .flatMap(s -> plugins.stream()
                    .filter(connectPlugin -> connectPlugin.getClassName().equals(s))
                    .findAny()
                )
        );
    }

    @Secured(Role.ROLE_CONNECT_INSERT)
    @Post(value = "create", consumes = MediaType.MULTIPART_FORM_DATA)
    public HttpResponse createSubmit(String cluster,
                                     String name,
                                     String transformsValue,
                                     Map<String, String> configs)
        throws Throwable
    {
        Map<String, String> validConfigs = ConnectRepository.validConfigs(configs, transformsValue);

        MutableHttpResponse<Void> response = HttpResponse.ok();

        Toast toast = this.toast(response, RequestHelper.runnableToToast(
            () -> this.connectRepository.create(cluster, name, validConfigs),
            "Definition '" + name + "' is created",
            "Failed to create definition '" + name + "'"
        ));

        URI redirect;

        if (toast.getType() != Toast.Type.error) {
            redirect = this.uri("/" + cluster + "/connect/" + name);
        } else {
            redirect = this.uri("/" + cluster + "/connect/create"); // @TODO: redirect with class
        }

        return response.status(HttpStatus.MOVED_PERMANENTLY)
            .headers((headers) ->
                headers.location(redirect)
            );
    }

    @Secured(Role.ROLE_CONNECT_DELETE)
    @Get("{name}/delete")
    public HttpResponse delete(String cluster, String name) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
            this.connectRepository.delete(cluster, name),
            "Definition '" + name + "' is deleted",
            "Failed to delete definition from '" + name + "'"
        ));

        return response;
    }

    @Secured(Role.ROLE_CONNECT_UPDATE)
    @View("connect")
    @Get("{name}")
    public HttpResponse home(HttpRequest request, String cluster, String name) {
        return this.render(request, cluster, name, "tasks");
    }

    @Secured(Role.ROLE_CONNECT_UPDATE)
    @View("connect")
    @Get("{name}/{tab:(tasks|configs)}")
    public HttpResponse tabs(HttpRequest request, String cluster, String name, String tab) {
        return this.render(request, cluster, name, tab);
    }

    @Secured(Role.ROLE_CONNECT_UPDATE)
    @Post(value = "{name}/configs", consumes = MediaType.MULTIPART_FORM_DATA)
    public HttpResponse updateDefinition(String cluster,
                                         String name,
                                         String transformsValue,
                                         Map<String, String> configs) throws Throwable {
        MutableHttpResponse<Void> response = HttpResponse.redirect(this.uri("/" + cluster + "/connect"));
        Map<String, String> validConfigs = ConnectRepository.validConfigs(configs, transformsValue);

        this.toast(response, RequestHelper.runnableToToast(
            () -> this.connectRepository.update(cluster, name, validConfigs),
            "Definition '" + name + "' is updated",
            "Failed to update definition '" + name + "'"
        ));

        return response;
    }

    @Secured(Role.ROLE_CONNECT_STATE_UPDATE)
    @Get("{name}/restart")
    public HttpResponse definitionRestart(HttpRequest request, String cluster, String name) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
                this.connectRepository.restart(cluster, name),
            "Definition '" + name + "' restarted",
            "Failed to restart '" + name + "'"
        ));

        return response;
    }

    @Secured(Role.ROLE_CONNECT_STATE_UPDATE)
    @Get("{name}/pause")
    public HttpResponse definitionPause(HttpRequest request, String cluster, String name) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
                this.connectRepository.pause(cluster, name),
            "Definition '" + name + "' paused",
            "Failed to pause '" + name + "'"
        ));

        return response;
    }

    @Secured(Role.ROLE_CONNECT_STATE_UPDATE)
    @Get("{name}/resume")
    public HttpResponse definitionResume(HttpRequest request, String cluster, String name) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
                this.connectRepository.resume(cluster, name),
            "Definition '" + name + "' resumed",
            "Failed to resumed'" + name + "'"
        ));

        return response;
    }

    @Secured(Role.ROLE_CONNECT_STATE_UPDATE)
    @Get("{name}/tasks/{taskId}/restart")
    public HttpResponse taskRestart(HttpRequest request, String cluster, String name, int taskId) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
                this.connectRepository.restartTask(cluster, name, taskId),
            "Definition '" + name + "' tasks " + taskId + " restarted",
            "Failed to restart tasks " + taskId + " from '" + name + "'"
        ));

        return response;
    }

    private HttpResponse render(HttpRequest request, String cluster, String subject, String tab) {
        ConnectDefinition definition = this.connectRepository.getDefinition(cluster, subject);

        return this.template(
            request,
            cluster,
            "tab", tab,
            "definition", definition,
            "pluginDefinition", this.connectRepository.getPlugin(cluster, definition.getShortClassName())
        );
    }
}
