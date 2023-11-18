package org.akhq.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.TopicConfig;
import org.akhq.repositories.ConfigRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class Topic {
    private String name;
    private boolean internal;
    @JsonIgnore
    private boolean configInternal;
    @JsonIgnore
    private boolean configStream;
    private final List<Partition> partitions = new ArrayList<>();

    public Topic(
        TopicDescription description,
        List<LogDir> logDirs,
        List<Partition.Offsets> offsets,
        boolean configInternal,
        boolean configStream
    ) {
        this.name = description.name();
        this.internal = description.isInternal();

        this.configInternal = configInternal;
        this.configStream = configStream;

        for (TopicPartitionInfo partition : description.partitions()) {
            this.partitions.add(new Partition(
                description.name(),
                partition,
                logDirs.stream()
                    .filter(logDir -> logDir.getPartition() == partition.partition())
                    .collect(Collectors.toList()),
                offsets.stream()
                    .filter(offset -> offset.getPartition() == partition.partition())
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException(
                        "Partition Offsets '" + partition.partition() + "' doesn't exist for topic " + this.name
                    ))
            ));
        }
    }

    public boolean isInternalTopic() {
        return this.internal || this.configInternal;
    }

    public boolean isStreamTopic() {
        return this.configStream;
    }

    public long getReplicaCount() {
        return this.getPartitions().stream()
            .mapToLong(partitions -> partitions.getNodes().size())
            .max()
            .orElseGet(() -> 0L);
    }

    public long getInSyncReplicaCount() {
        return this.getPartitions().stream()
            .mapToLong(partition -> partition.getNodes().stream().filter(node -> node.isInSyncReplicas()).count())
            .min()
            .orElseGet(() -> 0L);
    }

    public List<LogDir> getLogDir() {
        return this.getPartitions().stream()
            .flatMap(partition -> partition.getLogDir().stream())
            .collect(Collectors.toList());
    }

    public Long getLogDirSize() {
        Integer logDirCount = this.getPartitions().stream()
            .map(r -> r.getLogDir().size())
            .reduce(0, Integer::sum);

        if (logDirCount == 0) {
            return null;
        }

        return Optional
            .of(this.getPartitions().stream()
                .map(Partition::getLogDirSize)
                .reduce(0L, Long::sum)
            )
            .orElse(null);
    }

    public long getSize() {
        return this.getPartitions().stream()
            .map(partition -> partition.getLastOffset() - partition.getFirstOffset())
            .reduce(0L, Long::sum);
    }

    public long getSize(int partition) {
        for (Partition current : this.getPartitions()) {
            if (partition == current.getId()) {
                return current.getLastOffset() - current.getFirstOffset();
            }
        }

        throw new NoSuchElementException("Partition '" + partition + "' doesn't exist for topic " + this.name);
    }

    public Boolean canDeleteRecords(String clusterId, ConfigRepository configRepository) throws ExecutionException, InterruptedException {
        if (this.isInternalTopic()) {
            return false;
        }

        return isCompacted(configRepository.findByTopic(clusterId, this.getName()));
    }

    public static boolean isCompacted(List<Config> configs) {
        return configs != null && configs
            .stream()
            .filter(config -> config.getName().equals(TopicConfig.CLEANUP_POLICY_CONFIG))
            .anyMatch(config -> config.getValue().contains(TopicConfig.CLEANUP_POLICY_COMPACT));
    }
}
