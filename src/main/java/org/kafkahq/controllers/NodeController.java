package org.kafkahq.controllers;

import com.google.inject.Inject;
import org.jooby.Request;
import org.jooby.Results;
import org.jooby.View;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.kafkahq.models.Config;
import org.kafkahq.models.Node;
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
    public View list(Request request) throws ExecutionException, InterruptedException {
        return this.template(
            request,
            Results
                .html("nodeList")
                .put("cluster", this.clusterRepository.get())
        );
    }

    @GET
    @Path("{id}")
    public View home(Request request) throws ExecutionException, InterruptedException {
        return this.node(request, "configs");
    }

    @GET
    @Path("{id}/{tab:(configs|logs)}")
    public View tab(Request request) throws ExecutionException, InterruptedException {
        return this.node(request, request.param("tab").value());
    }

    public View node(Request request, String tab) throws ExecutionException, InterruptedException {
        Node node = this.clusterRepository.get()
            .getNodes()
            .stream()
            .filter(e -> e.getId() == request.param("id").intValue())
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Node '" + request.param("id").intValue() + "' doesn't exist"));

        List<Config> configs = this.configRepository.findByBroker(request.param("id").value());

        return this.template(
            request,
            Results
                .html("node")
                .put("tab", tab)
                .put("node", node)
                .put("logs", logDirRepository.findByBroker(node.getId()))
                .put("configs", configs)
        );
    }

}
