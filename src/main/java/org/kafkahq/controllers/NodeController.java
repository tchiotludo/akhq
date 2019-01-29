package org.kafkahq.controllers;

import com.google.inject.Inject;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Results;
import org.jooby.View;
import org.jooby.mvc.GET;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.kafkahq.models.Config;
import org.kafkahq.models.Node;
import org.kafkahq.modules.RequestHelper;
import org.kafkahq.repositories.ClusterRepository;
import org.kafkahq.repositories.ConfigRepository;
import org.kafkahq.repositories.LogDirRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

@Path("/{cluster}/node")
public class NodeController extends AbstractController {

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private ConfigRepository configRepository;

    @Inject
    private LogDirRepository logDirRepository;

    @GET
    public View list(Request request, String cluster) throws ExecutionException, InterruptedException {
        return this.template(
            request,
            cluster,
            Results
                .html("nodeList")
                .put("cluster", this.clusterRepository.get())
        );
    }

    @GET
    @Path("{nodeId}")
    public View home(Request request, String cluster, Integer nodeId) throws ExecutionException, InterruptedException {
        return this.render(request, cluster, nodeId, "configs");
    }

    @GET
    @Path("{nodeId}/{tab:(logs)}")
    public View tab(Request request, String cluster, Integer nodeId, String tab) throws ExecutionException, InterruptedException {
        return this.render(request, cluster, nodeId, tab);
    }

    @POST
    @Path("{nodeId}")
    public void updateConfig(Request request, Response response, String cluster, Integer nodeId) throws Throwable {
        List<Config> updated = RequestHelper.updatedConfigs(request, this.configRepository.findByBroker(nodeId));

        this.toast(request, RequestHelper.runnableToToast(() -> {
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

        response.redirect(request.path());
    }
    
    public View render(Request request, String cluster, Integer nodeId, String tab) throws ExecutionException, InterruptedException {
        Node node = this.clusterRepository.get()
            .getNodes()
            .stream()
            .filter(e -> e.getId() == nodeId)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Node '" + nodeId + "' doesn't exist"));

        List<Config> configs = this.configRepository.findByBroker(nodeId);
        configs.sort((o1, o2) -> o1.isReadOnly() == o2.isReadOnly() ? 0 :
            (o1.isReadOnly() ? 1 : -1 )
        );

        return this.template(
            request,
            cluster,
            Results
                .html("node")
                .put("tab", tab)
                .put("node", node)
                .put("logs", logDirRepository.findByBroker(node.getId()))
                .put("configs", configs)
        );
    }

}
