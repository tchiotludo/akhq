package org.kafkahq.service.mapper;

import org.apache.commons.lang3.tuple.Pair;
import org.kafkahq.models.ConsumerGroup;
import org.kafkahq.models.Partition;
import org.kafkahq.models.Record;
import org.kafkahq.models.Topic;
import org.kafkahq.service.dto.topic.PartitionDTO;
import org.kafkahq.service.dto.topic.RecordDTO;
import org.kafkahq.service.dto.topic.TopicDTO;

import javax.inject.Singleton;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

@Singleton
public class TopicMapper {

    public TopicDTO fromTopicToTopicDTO(Topic topic) {
        List<ConsumerGroup> emptyList = new ArrayList<>();
        return new TopicDTO(topic.getName(), (int) topic.getSize(), Math.toIntExact(topic.getLogDirSize().get()), Integer.toString(topic.getPartitions().size()), Long.toString(topic.getReplicaCount()), Long.toString(topic.getInSyncReplicaCount()), (topic.getConsumerGroups().size() > 0) ? topic.getConsumerGroups() : emptyList);
    }

    public RecordDTO fromRecordToRecordDTO(Record record) {
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(record.getTimestamp()),
                TimeZone.getDefault().toZoneId());
        return new RecordDTO(record.getKeyAsString(), record.getValueAsString(), date, record.getPartition(),
                record.getOffset(), record.getHeaders(), Pair.of(record.getKeySchemaId(), record.getValueSchemaId()));
    }

    public PartitionDTO fromPartitionToPartitionDTO(Partition partition) {
        List<Pair<Integer, Boolean>> replicas = partition.getNodes()
                .stream()
                .map(nodePartition -> Pair.of(nodePartition.getId(), nodePartition.isInSyncReplicas()))
                .collect(Collectors.toList());

        Pair<Long, Long> offsets = Pair.of(partition.getFirstOffset(), partition.getLastOffset());
        Pair<Long, Long> size = Pair.of(partition.getLastOffset()-partition.getFirstOffset(), partition.getLogDirSize());

        return new PartitionDTO(partition.getId(), partition.getLeader().getId(), replicas, offsets, size);
    }
}
