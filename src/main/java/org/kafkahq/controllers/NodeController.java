package org.kafkahq.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.views.View;
import org.kafkahq.configs.Role;
import org.kafkahq.models.Config;
import org.kafkahq.models.Node;
import org.kafkahq.modules.RequestHelper;
import org.kafkahq.repositories.ClusterRepository;
import org.kafkahq.repositories.ConfigRepository;
import org.kafkahq.repositories.LogDirRepository;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

@Secured(Role.ROLE_NODE_READ)
@Controller("${kafkahq.server.base-path:}/{cluster}/node")
public class NodeController extends AbstractController {
    private ClusterRepository clusterRepository;
    private ConfigRepository configRepository;
    private LogDirRepository logDirRepository;

    @Inject
    public NodeController(ClusterRepository clusterRepository, ConfigRepository configRepository, LogDirRepository logDirRepository) {
        this.clusterRepository = clusterRepository;
        this.configRepository = configRepository;
        this.logDirRepository = logDirRepository;
    }

    @View("nodeList")
    @Get
    public HttpResponse list(HttpRequest request, String cluster) throws ExecutionException, InterruptedException {
        return this.template(
            request,
            cluster,
            "cluster", this.clusterRepository.get(cluster)
        );
    }

    @View("node")
    @Get("{nodeId}")
    public HttpResponse home(HttpRequest request, String cluster, Integer nodeId) throws ExecutionException, InterruptedException {
        return this.render(request, cluster, nodeId, "configs");
    }

    @View("node")
    @Get("{nodeId}/{tab:(logs)}")
    public HttpResponse tab(HttpRequest request, String cluster, Integer nodeId, String tab) throws ExecutionException, InterruptedException {
        return this.render(request, cluster, nodeId, tab);
    }

    @Secured(Role.ROLE_NODE_CONFIG_UPDATE)
    @Post(value = "{nodeId}", consumes = MediaType.MULTIPART_FORM_DATA)
    public HttpResponse updateConfig(HttpRequest request, String cluster, Integer nodeId, Map<String, String> configs) throws Throwable {
        List<Config> updated = ConfigRepository.updatedConfigs(configs, this.configRepository.findByBroker(cluster, nodeId));

        MutableHttpResponse<Void> response = HttpResponse.redirect(request.getUri());

        this.toast(response, RequestHelper.runnableToToast(() -> {
                if (updated.size() == 0) {
                    throw new IllegalArgumentException("No config to update");
                }

                this.configRepository.updateBroker(
                    cluster,
                    nodeId,
                    updated
                );
            },
            "Node configs '" + nodeId + "' is updated",
            "Failed to update node '" + nodeId + "' configs"
        ));

        return response;
    }

    private HttpResponse render(HttpRequest request, String cluster, Integer nodeId, String tab) throws ExecutionException, InterruptedException {
        Node node = this.clusterRepository.get(cluster)
            .getNodes()
            .stream()
            .filter(e -> e.getId() == nodeId)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Node '" + nodeId + "' doesn't exist"));

        List<Config> configs = this.configRepository.findByBroker(cluster, nodeId);
        configs.sort((o1, o2) -> o1.isReadOnly() == o2.isReadOnly() ? 0 :
            (o1.isReadOnly() ? 1 : -1 )
        );

        return this.template(
            request,
            cluster,
            "tab", tab,
            "node", node,
            "logs", logDirRepository.findByBroker(cluster, node.getId()),
            "configs", configs
        );
    }
}
