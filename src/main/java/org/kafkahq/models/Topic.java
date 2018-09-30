package org.kafkahq.models;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartitionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode
public class Topic {
    public Topic(
        TopicDescription description,
        List<ConsumerGroup> consumerGroup,
        List<LogDir> logDirs,
        List<Partition.Offsets> offsets
    ) {
        this.name = description.name();
        this.internal = description.isInternal();
        this.consumerGroups = consumerGroup;

        for (TopicPartitionInfo partition : description.partitions()) {
            this.partitions.add(new Partition(
                description.name(),
                partition,
                logDirs.stream()
                    .filter(logDir -> logDir.getPartition() == partition.partition())
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException(
                        "Partition '" + partition.partition() + "' doesn't exist for topic " + this.name
                    )),
                offsets.stream()
                    .filter(offset -> offset.getPartition() == partition.partition())
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException(
                        "Partition Offsets '" + partition.partition() + "' doesn't exist for topic " + this.name
                    ))
            ));
        }
    }

    private String name;

    public String getName() {
        return name;
    }

    private boolean internal;

    public boolean isInternal() {
        return internal;
    }

    private final List<Partition> partitions = new ArrayList<>();

    public List<Partition> getPartitions() {
        return partitions;
    }

    private List<ConsumerGroup> consumerGroups;

    public List<ConsumerGroup> getConsumerGroups() {
        return consumerGroups;
    }

    public List<Node.Partition> getReplicas() {
        return this.getPartitions().stream()
            .flatMap(partition -> partition.getNodes().stream())
            .distinct()
            .collect(Collectors.toList());
    }

    public List<Node.Partition> getInSyncReplicas() {
        return this.getPartitions().stream()
            .flatMap(partition -> partition.getNodes().stream())
            .filter(Node.Partition::isInSyncReplicas)
            .distinct()
            .collect(Collectors.toList());
    }

    public long getLogDirSize() {
        return this.getPartitions().stream()
            .map(p -> p.getLogDir().getSize())
            .reduce(0L, Long::sum);
    }

    public long getSumFirstOffsets() {
        return this.getPartitions().stream()
            .map(Partition::getFirstOffset)
            .reduce(0L, Long::sum);
    }

    public long getSumOffsets() {
        return this.getPartitions().stream()
            .map(Partition::getLastOffset)
            .reduce(0L, Long::sum);
    }
}
