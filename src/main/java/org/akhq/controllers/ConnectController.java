package org.akhq.controllers;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.swagger.v3.oas.annotations.Operation;
import org.akhq.configs.Role;
import org.akhq.models.ConnectDefinition;
import org.akhq.models.ConnectPlugin;
import org.akhq.repositories.ConnectRepository;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;
import org.akhq.utils.ResultPagedList;
import org.codehaus.httpcache4j.uri.URIBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

@Secured(Role.ROLE_CONNECT_READ)
@Controller("/api/{cluster}/connect/{connectId}")
public class ConnectController extends AbstractController {
    private final ConnectRepository connectRepository;

    // I used the same configuration as for the registry schema
    @Value("${akhq.pagination.page-size}")
    private Integer pageSize;

    @Inject
    public ConnectController(ConnectRepository connectRepository) {
        this.connectRepository = connectRepository;
    }

    @Get
    @Operation(tags = {"connect"}, summary = "List all connect definitions")
    public ResultPagedList<ConnectDefinition> list(
            HttpRequest<?> request, String cluster, String connectId,  Optional<String> search, Optional<Integer> page)
            throws IOException, RestClientException, ExecutionException, InterruptedException
    {
        URIBuilder uri = URIBuilder.fromURI(request.getUri());
        Pagination pagination = new Pagination(pageSize, uri, page.orElse(1));

        return ResultPagedList.of(this.connectRepository.getPaginatedDefinitions(cluster, connectId, pagination, search));
    }

    @Get("/plugins")
    @Operation(tags = {"connect"}, summary = "List all connect plugins")
    public List<ConnectPlugin> pluginsList(String cluster, String connectId) {
        return this.connectRepository.getPlugins(cluster, connectId);
    }

    @Get("/plugins/{type}")
    @Operation(tags = {"connect"}, summary = "Retrieve a connect plugin")
    public ConnectPlugin plugins(String cluster, String connectId, String type) {
        List<ConnectPlugin> plugins = this.connectRepository.getPlugins(cluster, connectId);

        return plugins
            .stream()
            .filter(connectPlugin -> connectPlugin.getClassName().equals(type))
            .findAny()
            .orElseThrow();
    }

    @Secured(Role.ROLE_CONNECT_INSERT)
    @Post
    @Operation(tags = {"connect"}, summary = "Create a new connect definition")
    public ConnectDefinition create(
        String cluster,
        String connectId,
        String name,
        Map<String, String> configs
    ) {
        return this.connectRepository.create(cluster, connectId, name, configs);
    }

    @Secured(Role.ROLE_CONNECT_DELETE)
    @Delete("/{name}")
    @Operation(tags = {"connect"}, summary = "Delete a connect definition")
    public HttpResponse<?> delete(String cluster, String connectId, String name) {
        this.connectRepository.delete(cluster, connectId, name);

        return HttpResponse.noContent();
    }

    @Get("/{name}")
    @Operation(tags = {"connect"}, summary = "Retrieve a connect definition")
    public ConnectDefinition home(HttpRequest<?> request, String cluster, String connectId, String name) {
        return this.connectRepository.getDefinition(cluster, connectId, name);
    }

    @Get("/{name}/tasks")
    @Operation(tags = {"connect"}, summary = "Retrieve a connect task")
    public List<ConnectDefinition.TaskDefinition> tasks(HttpRequest<?> request, String cluster, String connectId, String name) {
        return this.connectRepository.getDefinition(cluster, connectId, name).getTasks();
    }

    @Get("/{name}/configs")
    @Operation(tags = {"connect"}, summary = "Retrieve a connect config")
    public Map<String, String> configs(HttpRequest<?> request, String cluster, String connectId, String name) {
        return this.connectRepository.getDefinition(cluster, connectId, name).getConfigs();
    }

    @Secured(Role.ROLE_CONNECT_UPDATE)
    @Post(value = "/{name}/configs")
    @Operation(tags = {"connect"}, summary = "Update a connect definition config")
    public ConnectDefinition update(
        String cluster,
        String connectId,
        String name,
        Map<String, String> configs
    ) {
        return this.connectRepository.update(cluster, connectId, name, configs);
    }

    @Secured(Role.ROLE_CONNECT_STATE_UPDATE)
    @Get("/{name}/restart")
    @Operation(tags = {"connect"}, summary = "Restart a connect definition")
    public HttpResponse<?> definitionRestart(String cluster, String connectId, String name) {
        this.connectRepository.restart(cluster, connectId, name);

        return HttpResponse.noContent();
    }

    @Secured(Role.ROLE_CONNECT_STATE_UPDATE)
    @Get("/{name}/pause")
    @Operation(tags = {"connect"}, summary = "Pause a connect definition")
    public HttpResponse<?> definitionPause(String cluster, String connectId, String name) {
        this.connectRepository.pause(cluster, connectId, name);

        return HttpResponse.noContent();
    }

    @Secured(Role.ROLE_CONNECT_STATE_UPDATE)
    @Get("/{name}/resume")
    @Operation(tags = {"connect"}, summary = "Resume a connect definition")
    public HttpResponse<?> definitionResume(String cluster, String connectId, String name) {
        this.connectRepository.resume(cluster, connectId, name);

        return HttpResponse.noContent();
    }

    @Secured(Role.ROLE_CONNECT_STATE_UPDATE)
    @Get("/{name}/tasks/{taskId}/restart")
    @Operation(tags = {"connect"}, summary = "Restart a connect task")
    public HttpResponse<?> taskRestart(HttpRequest<?> request, String cluster, String connectId, String name, int taskId) {
        this.connectRepository.restartTask(cluster, connectId, name, taskId);

        return HttpResponse.noContent();
    }
}
