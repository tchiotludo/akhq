package org.kafkahq.service.mapper;

import org.apache.commons.lang3.tuple.Pair;
import org.kafkahq.models.ConsumerGroup;
import org.kafkahq.models.Record;
import org.kafkahq.models.Topic;
import org.kafkahq.service.dto.ConsumerGroupd.ConsumerGroupDTO;
import org.kafkahq.service.dto.topic.RecordDTO;

import javax.inject.Singleton;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

@Singleton
public class ConsumerGroupMapper {
    public ConsumerGroupDTO fromConsumerGroupToConsumerGroupDTO(ConsumerGroup consumerGroup) {
        List<String> emptyList = new ArrayList<>();
        return new ConsumerGroupDTO(consumerGroup.getId(),  consumerGroup.getState().toString(), consumerGroup.getCoordinator().getId(),consumerGroup.getMembers().size(), (consumerGroup.getTopics().size() > 0) ? consumerGroup.getTopics() : emptyList);
    }


    public RecordDTO fromRecordToRecordDTO(Record record) {
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(record.getTimestamp()),
                TimeZone.getDefault().toZoneId());
        return new RecordDTO(record.getKeyAsString(), record.getValueAsString(), date, record.getPartition(),
                record.getOffset(), record.getHeaders(), Pair.of(record.getKeySchemaId(), record.getValueSchemaId()));
    }


}
