package org.akhq.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.kafka.clients.admin.DescribeClusterResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class Cluster {
    private String id;
    private final List<Node> nodes = new ArrayList<>();
    private Node controller;

    public Cluster(DescribeClusterResult result) throws ExecutionException, InterruptedException {
        this.id = result.clusterId().get();
        for(org.apache.kafka.common.Node node : result.nodes().get()) {
            this.nodes.add(new Node(node));
        }

        if (result.controller().get() != null) {
            this.controller = new Node(result.controller().get());
        }
    }
}
