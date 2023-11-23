package org.akhq.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.kafka.common.TopicPartitionInfo;

import java.util.*;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class Partition {
    private Node.Partition leader;
    private int id;
    private String topic;
    private List<Node.Partition> nodes;
    private List<LogDir> logDir;
    private long firstOffset;
    private long lastOffset;

    public Partition(String topic, TopicPartitionInfo partitionInfo, List<LogDir> logDir, Offsets offsets) {
        this.id = partitionInfo.partition();
        this.topic = topic;
        this.logDir = logDir;
        this.firstOffset = offsets.getFirstOffset();
        this.lastOffset = offsets.getLastOffset();
        this.nodes = new ArrayList<>();
        for (org.apache.kafka.common.Node replica : partitionInfo.replicas()) {
            Node.Partition partition = new Node.Partition(
                replica,
                partitionInfo.leader().id() == replica.id(),
                partitionInfo.isr().stream().anyMatch(node -> node.id() == replica.id())
            );

            this.nodes.add(partition);
            if (partition.isLeader()) {
                this.leader = partition;
            }
        }

        if (this.leader == null) {
            org.apache.kafka.common.Node leader = partitionInfo.leader();
            this.leader = new Node.Partition(
                leader,
                true,
                partitionInfo.isr().stream().anyMatch(node -> node.id() == leader.id())
            );
        }
    }

    public Node.Partition getLeader() {
        return this.leader;
    }

    public long getLogDirSize() {
        return this.getLogDir().stream()
            .filter(logDir -> this.leader != null && logDir.getBrokerId() == this.leader.getId())
            .map(LogDir::getSize)
            .reduce(0L, Long::sum);
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @NoArgsConstructor
    public static class Offsets {
        private int partition;
        private long firstOffset;
        private long lastOffset;

        public Offsets(int partition, long start, long lastOffset) {
            this.partition = partition;
            this.firstOffset = start;
            this.lastOffset = lastOffset;
        }
    }
}
