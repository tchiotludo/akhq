package org.kafkahq.models;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.kafka.common.TopicPartitionInfo;

import java.util.*;

@ToString
@EqualsAndHashCode
public class Partition {
    public Partition(String topic, TopicPartitionInfo partitionInfo, LogDir logDir, Offsets offsets) {
        this.id = partitionInfo.partition();
        this.topic = topic;
        this.logDir = logDir;
        this.firstOffset = offsets.getFirstOffset();
        this.lastOffset = offsets.getLastOffset();
        this.nodes = new ArrayList<>();

        for (org.apache.kafka.common.Node replica : partitionInfo.replicas()) {
            nodes.add(new Node.Partition(
                replica,
                partitionInfo.leader().id() == replica.id(),
                partitionInfo.isr().stream().anyMatch(node -> node.id() == replica.id())
            ));
        }
    }

    private final int id;

    public int getId() {
        return id;
    }

    private final String topic;

    public String getTopic() {
        return topic;
    }

    private final List<Node.Partition> nodes;

    public List<Node.Partition> getNodes() {
        return nodes;
    }

    private final LogDir logDir;

    public LogDir getLogDir() {
        return logDir;
    }

    private final long firstOffset;

    public long getFirstOffset() {
        return firstOffset;
    }

    private final long lastOffset;

    public long getLastOffset() {
        return lastOffset;
    }

    public Node.Partition getLeader() {
        return nodes
            .stream()
            .filter(Node.Partition::isLeader)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Leader not found"));
    }

    @ToString
    @EqualsAndHashCode
    public static class Offsets
    {
        public Offsets(int partition, long start, long lastOffset) {
            this.partition = partition;
            this.firstOffset = start;
            this.lastOffset = lastOffset;
        }

        private final int partition;

        public int getPartition() {
            return partition;
        }

        private long firstOffset;

        public long getFirstOffset() {
            return firstOffset;
        }

        private long lastOffset;

        public long getLastOffset() {
            return lastOffset;
        }
    }
}
