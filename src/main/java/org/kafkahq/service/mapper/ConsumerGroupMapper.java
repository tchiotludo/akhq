package org.kafkahq.service.mapper;

import org.kafkahq.models.Consumer;
import org.kafkahq.models.ConsumerGroup;
import org.kafkahq.repositories.RecordRepository;
import org.kafkahq.service.dto.consumerGroup.ConsumerGroupMemberDTO;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.kafkahq.models.TopicPartition;
import org.kafkahq.service.dto.consumerGroup.ConsumerGroupDTO;
import org.kafkahq.service.dto.consumerGroup.ConsumerGroupOffsetDTO;
import org.kafkahq.service.dto.consumerGroup.GroupedTopicOffsetDTO;


@Singleton
public class ConsumerGroupMapper {
    public ConsumerGroupDTO fromConsumerGroupToConsumerGroupDTO(ConsumerGroup consumerGroup) {
        List<ConsumerGroupDTO.TopicLagDTO> emptyList = new ArrayList<>();
        List<ConsumerGroupDTO.TopicLagDTO> topicLags = new ArrayList<>();
        for (int i = 0; i < consumerGroup.getTopics().size(); i++) {
            ConsumerGroupDTO.TopicLagDTO topicLag = new ConsumerGroupDTO.TopicLagDTO(consumerGroup.getTopics().get(i), consumerGroup.getOffsetLag(consumerGroup.getTopics().get(i)));
            topicLags.add(topicLag);
        }
        return new ConsumerGroupDTO(consumerGroup.getId(), consumerGroup.getState().toString(), consumerGroup.getCoordinator().getId(), consumerGroup.getMembers().size(), (topicLags.size() > 0) ? topicLags : emptyList);
    }

    public ConsumerGroupMemberDTO fromConsumerGroupMemberToConsumerGroupMemberDTO(Consumer member) {
        List<ConsumerGroupMemberDTO.AssignmentDTO> assignments = new ArrayList<>();
        for (int i = 0; i < member.getAssignments().size(); i++) {
            ConsumerGroupMemberDTO.AssignmentDTO assignment = new ConsumerGroupMemberDTO.AssignmentDTO(member.getAssignments().get(i).getTopic(), member.getAssignments().get(i).getPartition());
            assignments.add(assignment);
        }
        return new ConsumerGroupMemberDTO(member.getClientId(), member.getId(), member.getHost(), assignments);
    }


    public ConsumerGroupOffsetDTO fromConsumerGroupToConsumerGroupOffsetDTO(TopicPartition.ConsumerGroupOffset offset) {

        ConsumerGroupOffsetDTO consumerGroupOffsetDTO = new ConsumerGroupOffsetDTO(offset.getTopic(), offset.getPartition(), offset.getMember().orElse(null), offset.getOffset().orElse(null), offset.getOffsetLag().orElse(null));
        return consumerGroupOffsetDTO;
    }

    public GroupedTopicOffsetDTO fromOffsetForTimeToGroupedTopicOffsetDTO(List<RecordRepository.TimeOffset> timeForOffset) {
        List<String> keys = new ArrayList<>();
        Map<String, List<GroupedTopicOffsetDTO.OffsetDTO>> map = new HashMap<>();

        timeForOffset.stream().map(offset -> {
            String topicId = offset.getTopic();
            if (!keys.contains(topicId)) {
                List<GroupedTopicOffsetDTO.OffsetDTO> offsets;
                keys.add(topicId);

                offsets = timeForOffset.stream().filter(elem -> elem.getTopic().equals(topicId)).map(elem -> {
                    return new GroupedTopicOffsetDTO.OffsetDTO(elem.getPartition(), elem.getOffset(), 0L, 0L);
                }).collect(Collectors.toList());

                map.put(topicId, offsets);
            }

            return null;
        });

        return new GroupedTopicOffsetDTO(map);
    }

    public GroupedTopicOffsetDTO fromGroupedTopicOffsetToGroupedTopicOffsetDTO(Map<String, List<TopicPartition.ConsumerGroupOffset>> groupedTopicOffset) {
        Map<String, List<GroupedTopicOffsetDTO.OffsetDTO>> map = new HashMap<>();

        for (String topicId : groupedTopicOffset.keySet()) {
            map.put(
                    topicId,
                    groupedTopicOffset
                            .get(topicId)
                            .stream()
                            .map(this::fromConsumerGroupOffsetToOffsetDTO)
                            .collect(Collectors.toList())
            );
        }

        return new GroupedTopicOffsetDTO(map);
    }

    private GroupedTopicOffsetDTO.OffsetDTO fromConsumerGroupOffsetToOffsetDTO(TopicPartition.ConsumerGroupOffset consumerGroupOffset) {
        return new GroupedTopicOffsetDTO.OffsetDTO(
                consumerGroupOffset.getPartition(),
                consumerGroupOffset.getOffset().orElse(0L),
                consumerGroupOffset.getFirstOffset().orElse(0L),
                consumerGroupOffset.getLastOffset().orElse(0L)
        );
    }
}
