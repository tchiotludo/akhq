package org.kafkahq.service.mapper;

import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.Node;
import org.kafkahq.models.ConsumerGroup;
import org.kafkahq.models.Topic;
import org.kafkahq.service.dto.TopicDTO;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Singleton
public class TopicMapper {

    public TopicDTO fromTopicToTopicDTO(Topic topic) {
        List<ConsumerGroup> emptyList = new ArrayList<>();
        return new TopicDTO(topic.getName(), (int)topic.getSize(), Math.toIntExact(topic.getLogDirSize().get()), Integer.toString(topic.getPartitions().size()), Long.toString(topic.getReplicaCount()) ,Long.toString(topic.getInSyncReplicaCount()), (topic.getConsumerGroups().size() >0) ? topic.getConsumerGroups() : emptyList);
    }
}
