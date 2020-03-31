package org.akhq.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class Node {
    private int id;
    private String host;
    private int port;
    private String rack;

    public Node(org.apache.kafka.common.Node node) {
        this.id = node.id();
        this.host = node.host();
        this.port = node.port();
        this.rack = node.rack();
    }

    @ToString
    @EqualsAndHashCode(callSuper=true)
    @Getter
    @NoArgsConstructor
    public static class Partition extends Node {
        private boolean isLeader;
        private boolean isInSyncReplicas;

        public Partition(org.apache.kafka.common.Node node, boolean isLeader, boolean isInSyncReplicas) {
            super(node);
            this.isLeader = isLeader;
            this.isInSyncReplicas = isInSyncReplicas;
        }
    }
}
