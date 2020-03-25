package org.akhq.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.kafka.common.TopicPartitionInfo;

import java.util.*;

@ToString
@EqualsAndHashCode
@Getter
public class Partition {
    private final int id;
    private final String topic;
    private final List<Node.Partition> nodes;
    private final List<LogDir> logDir;
    private final long firstOffset;
    private final long lastOffset;

    public Partition(String topic, TopicPartitionInfo partitionInfo, List<LogDir> logDir, Offsets offsets) {
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

    public Node.Partition getLeader() {
        return nodes
            .stream()
            .filter(Node.Partition::isLeader)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Leader not found"));
    }

    public long getLogDirSize() {
        return this.getLogDir().stream()
            .filter(logDir -> logDir.getBrokerId() == this.getLeader().getId())
            .map(LogDir::getSize)
            .reduce(0L, Long::sum);
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    public static class Offsets {
        private final int partition;
        private long firstOffset;
        private long lastOffset;

        public Offsets(int partition, long start, long lastOffset) {
            this.partition = partition;
            this.firstOffset = start;
            this.lastOffset = lastOffset;
        }
    }
}
