package org.kafkahq.service.mapper;

import org.apache.commons.lang3.tuple.Pair;
import org.kafkahq.models.*;
import org.kafkahq.service.dto.topic.LogDTO;
import org.kafkahq.service.dto.topic.PartitionDTO;
import org.kafkahq.service.dto.topic.PartitionDTO.OffsetsDTO;
import org.kafkahq.service.dto.topic.PartitionDTO.ReplicaDTO;
import org.kafkahq.service.dto.topic.PartitionDTO.SizesDTO;
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
        List<ReplicaDTO> replicas = partition.getNodes()
                .stream()
                .map(nodePartition -> new ReplicaDTO(nodePartition.getId(), nodePartition.isInSyncReplicas()))
                .collect(Collectors.toList());

        OffsetsDTO offsetsDTO = new OffsetsDTO(partition.getFirstOffset(), partition.getLastOffset());

        SizesDTO sizesDTO = new SizesDTO(partition.getLastOffset()-partition.getFirstOffset(), partition.getLogDirSize());

        return new PartitionDTO(partition.getId(), partition.getLeader().getId(), replicas, offsetsDTO, sizesDTO);
    }

    public LogDTO fromLogToLogDTO(LogDir log) {

        return new LogDTO(log.getBrokerId(),log.getTopic(),log.getPartition(),log.getSize(),log.getOffsetLag());
    }


}
