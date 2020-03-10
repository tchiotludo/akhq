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
        List<ConsumerGroupDTO.TopicLagDTO> emptyList = new ArrayList<>();
        List<ConsumerGroupDTO.TopicLagDTO> topicLags= new ArrayList<>();
        for(int i=0; i<consumerGroup.getTopics().size();i++){
            ConsumerGroupDTO.TopicLagDTO topicLag= new ConsumerGroupDTO.TopicLagDTO(consumerGroup.getTopics().get(i),consumerGroup.getOffsetLag(consumerGroup.getTopics().get(i)));
            topicLags.add(topicLag);
        }
        return new ConsumerGroupDTO(consumerGroup.getId(),  consumerGroup.getState().toString(), consumerGroup.getCoordinator().getId(),consumerGroup.getMembers().size(), (topicLags.size() > 0) ? topicLags : emptyList);

    }
}
