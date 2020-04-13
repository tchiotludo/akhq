package org.akhq.service.mapper;

import org.apache.commons.lang3.tuple.Pair;
import org.akhq.models.*;
import org.akhq.service.dto.topic.ConfigDTO;
import org.akhq.service.dto.topic.LogDTO;
import org.akhq.service.dto.topic.PartitionDTO;
import org.akhq.service.dto.topic.PartitionDTO.OffsetsDTO;
import org.akhq.service.dto.topic.PartitionDTO.ReplicaDTO;
import org.akhq.service.dto.topic.PartitionDTO.SizesDTO;
import org.akhq.service.dto.topic.RecordDTO;
import org.akhq.service.dto.topic.TopicDTO;

import javax.inject.Singleton;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

@Singleton
public class TopicMapper {
    private static final String CONFIG_FORMAT = "configs[%s]";

    public TopicDTO fromTopicToTopicDTO(Topic topic) {
        List<ConsumerGroup> emptyList = new ArrayList<>();
        return new TopicDTO(topic.getName(), (int) topic.getSize(), Math.toIntExact(topic.getLogDirSize().get()), Integer.toString(topic.getPartitions().size()), Long.toString(topic.getReplicaCount()), Long.toString(topic.getInSyncReplicaCount()), (topic.getConsumerGroups().size() > 0) ? topic.getConsumerGroups() : emptyList);
    }

    public RecordDTO fromRecordToRecordDTO(Record record) {
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(record.getTimestamp()),
                TimeZone.getDefault().toZoneId());
        return new RecordDTO(record.getKey(), record.getKey(), date, record.getPartition(),
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

    public ConfigDTO fromConfigToConfigDTO(Config config) {
        ConfigDTO.DataType dataType;
        try {
            switch (config.getName().substring(config.getName().lastIndexOf("."))) {
                case ".ms":
                    dataType = ConfigDTO.DataType.MILLI;
                    break;
                case ".size":
                    dataType = ConfigDTO.DataType.BYTES;
                    break;
                default:
                    dataType = ConfigDTO.DataType.TEXT;
                    break;
            }
        } catch (StringIndexOutOfBoundsException ex) {
            dataType = ConfigDTO.DataType.TEXT;
        }

        return new ConfigDTO(config.getName(), config.getValue(), config.getDescription(),
                ConfigDTO.Source.valueOf(config.getSource().name()), dataType, config.isReadOnly(),
                config.isSensitive());
    }

    public Map<String, String> convertConfigsMap(Map<String, String> configs) {
        return configs.entrySet().stream().collect(Collectors.toMap(
                e -> String.format(CONFIG_FORMAT, e.getKey()),
                Map.Entry::getValue
        ));
    }
}
