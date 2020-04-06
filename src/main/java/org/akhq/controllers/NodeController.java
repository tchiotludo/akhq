package org.akhq.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.views.View;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import org.akhq.configs.Role;
import org.akhq.models.Cluster;
import org.akhq.models.Config;
import org.akhq.models.LogDir;
import org.akhq.models.Node;
import org.akhq.modules.RequestHelper;
import org.akhq.repositories.ClusterRepository;
import org.akhq.repositories.ConfigRepository;
import org.akhq.repositories.LogDirRepository;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

@Secured(Role.ROLE_NODE_READ)
@Controller("${akhq.server.base-path:}/")
public class NodeController extends AbstractController {
    private final ClusterRepository clusterRepository;
    private final ConfigRepository configRepository;
    private final LogDirRepository logDirRepository;

    @Inject
    public NodeController(ClusterRepository clusterRepository, ConfigRepository configRepository, LogDirRepository logDirRepository) {
        this.clusterRepository = clusterRepository;
        this.configRepository = configRepository;
        this.logDirRepository = logDirRepository;
    }

    @View("nodeList")
    @Get("{cluster}/node")
    @Hidden
    public HttpResponse<?> list(HttpRequest<?> request, String cluster) throws ExecutionException, InterruptedException {
        return this.template(
            request,
            cluster,
            "cluster", this.clusterRepository.get(cluster)
        );
    }

    @Get("api/{cluster}/node")
    @Operation(tags = {"node"}, summary = "List all nodes")
    public Cluster listApi(String cluster) throws ExecutionException, InterruptedException {
        return this.clusterRepository.get(cluster);
    }

    @View("node")
    @Get("{cluster}/node/{nodeId}")
    @Hidden
    public HttpResponse<?> home(HttpRequest<?> request, String cluster, Integer nodeId) throws ExecutionException, InterruptedException {
        return this.render(request, cluster, nodeId, "configs");
    }

    @Get("api/{cluster}/node/{nodeId}")
    @Operation(tags = {"node"}, summary = "Retrieve a nodes")
    public Node nodeApi(String cluster, Integer nodeId) throws ExecutionException, InterruptedException {
        return findNode(cluster, nodeId);
    }

    @View("node")
    @Get("{cluster}/node/{nodeId}/{tab:(logs)}")
    @Hidden
    public HttpResponse<?> tab(HttpRequest<?> request, String cluster, Integer nodeId, String tab) throws ExecutionException, InterruptedException {
        return this.render(request, cluster, nodeId, tab);
    }

    @Get("api/{cluster}/node/{nodeId}/logs")
    @Operation(tags = {"node"}, summary = "List all logs for a node")
    public List<LogDir> nodeLogApi(String cluster, Integer nodeId) throws ExecutionException, InterruptedException {
        return logDirRepository.findByBroker(cluster, nodeId);
    }

    @Get("api/{cluster}/node/{nodeId}/configs")
    @Operation(tags = {"node"}, summary = "List all configs for a node")
    public List<Config> nodeConfigApi(String cluster, Integer nodeId) throws ExecutionException, InterruptedException {
        return nodeConfig(cluster, nodeId);
    }

    @Secured(Role.ROLE_NODE_CONFIG_UPDATE)
    @Post(value = "{cluster}/node/{nodeId}", consumes = MediaType.MULTIPART_FORM_DATA)
    @Hidden
    public HttpResponse<?> updateConfig(HttpRequest<?> request, String cluster, Integer nodeId, Map<String, String> configs) throws Throwable {
        List<Config> updated = ConfigRepository.updatedConfigs(configs, this.configRepository.findByBroker(cluster, nodeId), true);

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

    @Post("api/{cluster}/node/{nodeId}/configs")
    @Operation(tags = {"node"}, summary = "Update configs for a node")
    public List<Config> nodeConfigUpdateApi(String cluster, Integer nodeId, Map<String, String> configs) throws ExecutionException, InterruptedException {
        List<Config> updated = ConfigRepository.updatedConfigs(configs, this.configRepository.findByBroker(cluster, nodeId), false);

        if (updated.size() == 0) {
            throw new IllegalArgumentException("No config to update");
        }

        this.configRepository.updateBroker(
            cluster,
            nodeId,
            updated
        );

        return updated;
    }

    private HttpResponse<?> render(HttpRequest<?> request, String cluster, Integer nodeId, String tab) throws ExecutionException, InterruptedException {
        Node node = findNode(cluster, nodeId);

        List<Config> configs = nodeConfig(cluster, nodeId);

        return this.template(
            request,
            cluster,
            "tab", tab,
            "node", node,
            "logs", logDirRepository.findByBroker(cluster, node.getId()),
            "configs", configs
        );
    }

    private Node findNode(String cluster, Integer nodeId) throws ExecutionException, InterruptedException {
        return this.clusterRepository.get(cluster)
                .getNodes()
                .stream()
                .filter(e -> e.getId() == nodeId)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Node '" + nodeId + "' doesn't exist"));
    }

    private List<Config> nodeConfig(String cluster, Integer nodeId) throws ExecutionException, InterruptedException {
        List<Config> configs = this.configRepository.findByBroker(cluster, nodeId);

        configs.sort((o1, o2) -> o1.isReadOnly() == o2.isReadOnly() ? 0 :
            (o1.isReadOnly() ? 1 : -1 )
        );

        return configs;
    }
}
