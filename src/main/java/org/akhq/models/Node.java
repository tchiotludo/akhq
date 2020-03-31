package org.akhq.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Getter
public class Node {
    private final int id;
    private final String host;
    private final int port;
    private final String rack;

    public Node(org.apache.kafka.common.Node node) {
        this.id = node.id();
        this.host = node.host();
        this.port = node.port();
        this.rack = node.rack();
    }

    @ToString
    @EqualsAndHashCode(callSuper=true)
    @Getter
    public static class Partition extends Node {
        private final boolean isLeader;
        private final boolean isInSyncReplicas;

        public Partition(org.apache.kafka.common.Node node, boolean isLeader, boolean isInSyncReplicas) {
            super(node);
            this.isLeader = isLeader;
            this.isInSyncReplicas = isInSyncReplicas;
        }
    }
}
