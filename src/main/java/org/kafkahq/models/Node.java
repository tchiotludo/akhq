package org.kafkahq.models;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class Node {
    public Node(org.apache.kafka.common.Node node) {
        this.id = node.id();
        this.host = node.host();
        this.port = node.port();
        this.rack = node.rack();
    }

    private final int id;

    public int getId() {
        return id;
    }

    private final String host;

    public String getHost() {
        return host;
    }

    private final int port;

    public int getPort() {
        return port;
    }

    private final String rack;

    public String getRack() {
        return rack;
    }

    @ToString
    @EqualsAndHashCode(callSuper=true)
    public static class Partition extends Node
    {
        public Partition(org.apache.kafka.common.Node node, boolean isLeader, boolean isInSyncReplicas) {
            super(node);
            this.isLeader = isLeader;
            this.isInSyncReplicas = isInSyncReplicas;
        }

        private final boolean isLeader;

        public boolean isLeader() {
            return isLeader;
        }

        private final boolean isInSyncReplicas;

        public boolean isInSyncReplicas() {
            return isInSyncReplicas;
        }
    }
}
