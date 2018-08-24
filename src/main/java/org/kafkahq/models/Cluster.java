package org.kafkahq.models;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.kafka.clients.admin.DescribeClusterResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@ToString
@EqualsAndHashCode
public class Cluster {
    public Cluster(DescribeClusterResult result) throws ExecutionException, InterruptedException {
        this.id = result.clusterId().get();
        for(org.apache.kafka.common.Node node : result.nodes().get()) {
            this.nodes.add(new Node(node));
        }

        this.controller = new Node(result.controller().get());
    }

    private final String id;

    public String getId() {
        return id;
    }

    private final List<Node> nodes = new ArrayList<>();

    public List<Node> getNodes() {
        return nodes;
    }

    private final Node controller;

    public Node getController() {
        return controller;
    }
}
