package org.akhq.controllers;

import io.micronaut.http.*;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.views.View;
import io.swagger.annotations.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.akhq.configs.Role;
import org.akhq.models.ConnectDefinition;
import org.akhq.models.ConnectPlugin;
import org.akhq.modules.RequestHelper;
import org.akhq.repositories.ConnectRepository;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Secured(Role.ROLE_CONNECT_READ)
@Controller("${akhq.server.base-path:}/")
public class ConnectController extends AbstractController {
    private final ConnectRepository connectRepository;

    @Inject
    public ConnectController(ConnectRepository connectRepository) {
        this.connectRepository = connectRepository;
    }

    @View("connectList")
    @Get("{cluster}/connect/{connectId}")
    @Hidden
    public HttpResponse<?> list(HttpRequest<?> request, String cluster, String connectId) {
        return this.template(
            request,
            cluster,
            "connectId", connectId,
            "connects", this.connectRepository.getDefinitions(cluster, connectId)
        );
    }

    @Get("api/{cluster}/connect/{connectId}")
    @Operation(tags = {"connect"}, summary = "List all connect definitions")
    public List<ConnectDefinition> listApi(String cluster, String connectId) {
        return this.connectRepository.getDefinitions(cluster, connectId);
    }

    @Get("api/{cluster}/connect/{connectId}/plugins")
    @Operation(tags = {"connect"}, summary = "List all connect plugins")
    public List<ConnectPlugin> pluginsListApi(String cluster, String connectId) {
        return this.connectRepository.getPlugins(cluster, connectId);
    }

    @Get("api/{cluster}/connect/{connectId}/plugins/{type}")
    @Operation(tags = {"connect"}, summary = "Retrieve a connect plugin")
    public ConnectPlugin pluginsApi(String cluster, String connectId, String type) {
        List<ConnectPlugin> plugins = this.connectRepository.getPlugins(cluster, connectId);

        return plugins
            .stream()
            .filter(connectPlugin -> connectPlugin.getClassName().equals(type))
            .findAny()
            .orElseThrow();
    }

    @Secured(Role.ROLE_CONNECT_INSERT)
    @View("connectCreate")
    @Get("{cluster}/connect/{connectId}/create")
    @Hidden
    public HttpResponse<?> create(HttpRequest<?> request, String cluster, Optional<String> type, String connectId) {
        List<ConnectPlugin> plugins = this.connectRepository.getPlugins(cluster, connectId);

        return this.template(
            request,
            cluster,
            "connectId", connectId,
            "plugins", plugins,
            "pluginDefinition", type
                .flatMap(s -> plugins.stream()
                    .filter(connectPlugin -> connectPlugin.getClassName().equals(s))
                    .findAny()
                )
        );
    }

    @Secured(Role.ROLE_CONNECT_INSERT)
    @Post(value = "{cluster}/connect/{connectId}/create", consumes = MediaType.MULTIPART_FORM_DATA)
    @Hidden
    public HttpResponse<?> createSubmit(
        String cluster,
        String connectId,
        String name,
        String transformsValue,
        Map<String, String> configs
    ) throws Throwable {
        Map<String, String> validConfigs = ConnectRepository.validConfigs(configs, transformsValue);

        MutableHttpResponse<Void> response = HttpResponse.ok();

        Toast toast = this.toast(response, RequestHelper.runnableToToast(
            () -> this.connectRepository.create(cluster, connectId, name, validConfigs),
            "Definition '" + name + "' is created",
            "Failed to create definition '" + name + "'"
        ));

        URI redirect;

        if (toast.getType() != Toast.Type.error) {
            redirect = this.uri("/" + cluster + "/connect/" + connectId + "/" + name);
        } else {
            redirect = this.uri("/" + cluster + "/connect/" + connectId  + "/create"); // @TODO: redirect with class
        }

        return response.status(HttpStatus.MOVED_PERMANENTLY)
            .headers((headers) ->
                headers.location(redirect)
            );
    }

    @Secured(Role.ROLE_CONNECT_INSERT)
    @Post(value = "api/{cluster}/connect/{connectId}")
    @Operation(tags = {"connect"}, summary = "Create a new connect definition")
    public ConnectDefinition createApi(
        String cluster,
        String connectId,
        String name,
        Map<String, String> configs
    ) {
        return this.connectRepository.create(cluster, connectId, name, configs);
    }

    @Secured(Role.ROLE_CONNECT_DELETE)
    @Get("{cluster}/connect/{connectId}/{name}/delete")
    @Hidden
    public HttpResponse<?> delete(String cluster, String connectId, String name) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
            this.connectRepository.delete(cluster, connectId, name),
            "Definition '" + name + "' is deleted",
            "Failed to delete definition from '" + name + "'"
        ));

        return response;
    }

    @Secured(Role.ROLE_CONNECT_DELETE)
    @Delete("api/{cluster}/connect/{connectId}/{name}")
    @Operation(tags = {"connect"}, summary = "Delete a connect definition")
    public HttpResponse<?> deleteApi(String cluster, String connectId, String name) {
        this.connectRepository.delete(cluster, connectId, name);

        return HttpResponse.noContent();
    }

    @Secured(Role.ROLE_CONNECT_UPDATE)
    @View("connect")
    @Get("{cluster}/connect/{connectId}/{name}")
    @Hidden
    public HttpResponse<?> home(HttpRequest<?> request, String cluster, String connectId, String name) {
        return this.render(request, cluster, connectId, name, "tasks");
    }

    @Get("api/{cluster}/connect/{connectId}/{name}")
    @Operation(tags = {"connect"}, summary = "Retrieve a connect definition")
    public ConnectDefinition homeApi(HttpRequest<?> request, String cluster, String connectId, String name) {
        return this.connectRepository.getDefinition(cluster, connectId, name);
    }

    @Get("api/{cluster}/connect/{connectId}/{name}/tasks")
    @Operation(tags = {"connect"}, summary = "Retrieve a connect task")
    public List<ConnectDefinition.TaskDefinition> tasksApi(HttpRequest<?> request, String cluster, String connectId, String name) {
        return this.connectRepository.getDefinition(cluster, connectId, name).getTasks();
    }

    @Get("api/{cluster}/connect/{connectId}/{name}/configs")
    @Operation(tags = {"connect"}, summary = "Retrieve a connect config")
    public Map<String, String> configsApi(HttpRequest<?> request, String cluster, String connectId, String name) {
        return this.connectRepository.getDefinition(cluster, connectId, name).getConfigs();
    }

    @Secured(Role.ROLE_CONNECT_UPDATE)
    @View("connect")
    @Get("{cluster}/connect/{connectId}/{name}/{tab:(tasks|configs)}")
    @Hidden
    public HttpResponse<?> tabs(HttpRequest<?> request, String cluster, String connectId, String name, String tab) {
        return this.render(request, cluster, connectId, name, tab);
    }

    @Secured(Role.ROLE_CONNECT_UPDATE)
    @Post(value = "{cluster}/connect/{connectId}/{name}/configs", consumes = MediaType.MULTIPART_FORM_DATA)
    @Hidden
    public HttpResponse<?> updateDefinition(
        String cluster,
        String connectId,
        String name,
        String transformsValue,
        Map<String, String> configs
    ) throws Throwable {
        MutableHttpResponse<Void> response = HttpResponse.redirect(this.uri("/" + cluster + "/connect/" + connectId));
        Map<String, String> validConfigs = ConnectRepository.validConfigs(configs, transformsValue);

        this.toast(response, RequestHelper.runnableToToast(
            () -> this.connectRepository.update(cluster, connectId, name, validConfigs),
            "Definition '" + name + "' is updated",
            "Failed to update definition '" + name + "'"
        ));

        return response;
    }

    @Secured(Role.ROLE_CONNECT_UPDATE)
    @Post(value = "api/{cluster}/connect/{connectId}/{name}/configs")
    @Operation(tags = {"connect"}, summary = "Update a connect definition config")
    public ConnectDefinition updateApi(
        String cluster,
        String connectId,
        String name,
        Map<String, String> configs
    ) {
        return this.connectRepository.update(cluster, connectId, name, configs);
    }

    @Secured(Role.ROLE_CONNECT_STATE_UPDATE)
    @Get("{cluster}/connect/{connectId}/{name}/restart")
    @Hidden
    public HttpResponse<?> definitionRestart(HttpRequest<?> request, String cluster, String connectId, String name) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
                this.connectRepository.restart(cluster, connectId, name),
            "Definition '" + name + "' restarted",
            "Failed to restart '" + name + "'"
        ));

        return response;
    }

    @Secured(Role.ROLE_CONNECT_STATE_UPDATE)
    @Get("api/{cluster}/connect/{connectId}/{name}/restart")
    @Operation(tags = {"connect"}, summary = "Restart a connect definition")
    public HttpResponse<?> definitionRestartApi(String cluster, String connectId, String name) {
        this.connectRepository.restart(cluster, connectId, name);

        return HttpResponse.noContent();
    }

    @Secured(Role.ROLE_CONNECT_STATE_UPDATE)
    @Get("{cluster}/connect/{connectId}/{name}/pause")
    @Hidden
    public HttpResponse<?> definitionPause(HttpRequest<?> request, String cluster, String connectId, String name) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
                this.connectRepository.pause(cluster, connectId, name),
            "Definition '" + name + "' paused",
            "Failed to pause '" + name + "'"
        ));

        return response;
    }

    @Secured(Role.ROLE_CONNECT_STATE_UPDATE)
    @Get("api/{cluster}/connect/{connectId}/{name}/pause")
    @Operation(tags = {"connect"}, summary = "Pause a connect definition")
    public HttpResponse<?> definitionPauseApi(String cluster, String connectId, String name) {
        this.connectRepository.pause(cluster, connectId, name);

        return HttpResponse.noContent();
    }

    @Secured(Role.ROLE_CONNECT_STATE_UPDATE)
    @Get("{cluster}/connect/{connectId}/{name}/resume")
    @Hidden
    public HttpResponse<?> definitionResume(HttpRequest<?> request, String cluster, String connectId, String name) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
                this.connectRepository.resume(cluster, connectId, name),
            "Definition '" + name + "' resumed",
            "Failed to resumed'" + name + "'"
        ));

        return response;
    }

    @Secured(Role.ROLE_CONNECT_STATE_UPDATE)
    @Get("api/{cluster}/connect/{connectId}/{name}/resume")
    @Operation(tags = {"connect"}, summary = "Resume a connect definition")
    public HttpResponse<?> definitionResumeApi(String cluster, String connectId, String name) {
        this.connectRepository.resume(cluster, connectId, name);

        return HttpResponse.noContent();
    }

    @Secured(Role.ROLE_CONNECT_STATE_UPDATE)
    @Get("{cluster}/connect/{connectId}/{name}/tasks/{taskId}/restart")
    @Hidden
    public HttpResponse<?> taskRestart(HttpRequest<?> request, String cluster, String connectId, String name, int taskId) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
                this.connectRepository.restartTask(cluster, connectId, name, taskId),
            "Definition '" + name + "' tasks " + taskId + " restarted",
            "Failed to restart tasks " + taskId + " from '" + name + "'"
        ));

        return response;
    }

    @Secured(Role.ROLE_CONNECT_STATE_UPDATE)
    @Get("api/{cluster}/connect/{connectId}/{name}/tasks/{taskId}/restart")
    @Operation(tags = {"connect"}, summary = "Restart a connect task")
    public HttpResponse<?> taskRestartApi(HttpRequest<?> request, String cluster, String connectId, String name, int taskId) {
        this.connectRepository.restartTask(cluster, connectId, name, taskId);

        return HttpResponse.noContent();
    }

    private HttpResponse<?> render(HttpRequest<?> request, String cluster, String connectId, String subject, String tab) {
        ConnectDefinition definition = this.connectRepository.getDefinition(cluster, connectId, subject);

        return this.template(
            request,
            cluster,
                "connectId", connectId,
            "tab", tab,
            "definition", definition,
            "pluginDefinition", this.connectRepository.getPlugin(cluster, connectId, definition.getShortClassName())
        );
    }
}
