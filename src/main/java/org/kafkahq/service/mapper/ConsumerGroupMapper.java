package org.kafkahq.service.mapper;
import org.kafkahq.models.Consumer;
import org.kafkahq.models.ConsumerGroup;
import org.kafkahq.service.dto.ConsumerGroupd.ConsumerGroupMemberDTO;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import org.kafkahq.models.TopicPartition;
import org.kafkahq.service.dto.ConsumerGroup.ConsumerGroupDTO;
import org.kafkahq.service.dto.ConsumerGroup.ConsumerGroupOffsetDTO;


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

    public ConsumerGroupMemberDTO fromConsumerGroupMemberToConsumerGroupMemberDTO(Consumer member ){
        List<ConsumerGroupMemberDTO.AssignmentDTO> assignments= new ArrayList<>();
        for(int i =0; i<member.getAssignments().size();i++){
 ConsumerGroupMemberDTO.AssignmentDTO assignment= new  ConsumerGroupMemberDTO.AssignmentDTO(member.getAssignments().get(i).getTopic(),member.getAssignments().get(i).getPartition());
       assignments.add(assignment);
        }

       return new ConsumerGroupMemberDTO(member.getClientId(),member.getId(),member.getHost(),assignments);
    }


    public  ConsumerGroupOffsetDTO fromConsumerGroupToConsumerGroupOffsetDTO(TopicPartition.ConsumerGroupOffset offset) {

            ConsumerGroupOffsetDTO consumerGroupOffsetDTO=new ConsumerGroupOffsetDTO(offset.getTopic(),offset.getPartition(),offset.getMember().orElse(null),offset.getOffset().orElse(null), offset.getOffsetLag().orElse(null));



        return  consumerGroupOffsetDTO;
    }




}
