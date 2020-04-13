package org.akhq.service;

import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import org.akhq.models.Consumer;
import org.akhq.models.ConsumerGroup;
import org.akhq.models.TopicPartition;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.modules.KafkaModule;
import org.akhq.repositories.ConsumerGroupRepository;
import org.akhq.repositories.RecordRepository;
import org.akhq.service.dto.acls.AclsDTO;
import org.akhq.service.dto.consumerGroup.ConsumerGroupDTO;
import org.akhq.service.dto.consumerGroup.ConsumerGroupListDTO;
import org.akhq.service.dto.consumerGroup.ConsumerGroupMemberDTO;
import org.akhq.service.dto.consumerGroup.ConsumerGroupOffsetDTO;
import org.akhq.service.dto.consumerGroup.ConsumerGroupUpdateDTO;
import org.akhq.service.dto.consumerGroup.GroupedTopicOffsetDTO;
import org.akhq.service.mapper.ConsumerGroupMapper;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;
import org.apache.kafka.common.resource.ResourceType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class ConsumerGroupService {
    private KafkaModule kafkaModule;
    private AbstractKafkaWrapper kafkaWrapper;
    private Environment environment;

    private ConsumerGroupRepository consumerGroupRepository;
    private RecordRepository recordRepository;
//    private AclService aclService;

    private ConsumerGroupMapper consumerGroupMapper;
    @Value("${akhq.topic.default-view}")
    private String defaultView;
    @Value("${akhq.pagination.page-size}")
    private Integer pageSize;

    @Inject
    public ConsumerGroupService(KafkaModule kafkaModule, ConsumerGroupMapper consumerGroupMapper, AbstractKafkaWrapper kafkaWrapper,
                                ConsumerGroupRepository consumerGroupRepository, Environment environment,
                                RecordRepository recordRepository
//            ,
//                                AclService aclService
    ) {
        this.kafkaModule = kafkaModule;
        this.consumerGroupMapper = consumerGroupMapper;
        this.kafkaWrapper = kafkaWrapper;
        this.consumerGroupRepository = consumerGroupRepository;
        this.environment = environment;
        this.recordRepository = recordRepository;
//        this.aclService = aclService;
    }

    public ConsumerGroupListDTO getConsumerGroup(String clusterId, Optional<String> search, Optional<Integer> pageNumber)
            throws ExecutionException, InterruptedException {
        Pagination pagination = new Pagination(pageSize, pageNumber.orElse(1
        ));

        PagedList<ConsumerGroup> list = this.consumerGroupRepository.list(clusterId, pagination, search);


        ArrayList<ConsumerGroupDTO> consumerGroupList = new ArrayList<>();
        list.stream().map(consumerGroup -> consumerGroupList.add(consumerGroupMapper.fromConsumerGroupToConsumerGroupDTO(consumerGroup))).collect(Collectors.toList());

        return new ConsumerGroupListDTO(consumerGroupList, list.pageCount());
    }


    public List<ConsumerGroupOffsetDTO> getConsumerGroupOffsets(String clusterId, String groupId) throws ExecutionException, InterruptedException {

        ConsumerGroup group = this.consumerGroupRepository.findByName(clusterId, groupId);
        List<TopicPartition.ConsumerGroupOffset> offsets = group.getOffsets();
        List<ConsumerGroupOffsetDTO> offsetsDTO = new ArrayList<>();

        for (int i = 0; i < offsets.size(); i++) {
            ConsumerGroupOffsetDTO offsetDTO = consumerGroupMapper.fromConsumerGroupToConsumerGroupOffsetDTO(offsets.get(i));
            offsetsDTO.add(offsetDTO);
        }

        return offsetsDTO;

    }

    public void deleteConsumerGroup(String clusterId, String consumerGroupId) throws ExecutionException, InterruptedException {
        kafkaWrapper.deleteConsumerGroups(clusterId, consumerGroupId);
    }

    public GroupedTopicOffsetDTO getConsumerGroupGroupedTopicOffsets(
            String clusterId,
            String groupId,
            Optional<String> timestamp)
            throws ExecutionException, InterruptedException {

        ConsumerGroup group = this.consumerGroupRepository.findByName(clusterId, groupId);

        List<RecordRepository.TimeOffset> offsetForTime;
        if (timestamp.isPresent() && (offsetForTime = recordRepository.getOffsetForTime(
                clusterId,
                group.getOffsets()
                        .stream()
                        .map(r -> new TopicPartition(r.getTopic(), r.getPartition()))
                        .collect(Collectors.toList()),
                Instant.parse(timestamp.get()).toEpochMilli()
        )).size() > 0) {
            return consumerGroupMapper.fromOffsetForTimeToGroupedTopicOffsetDTO(offsetForTime);
        } else {
            Map<String, List<TopicPartition.ConsumerGroupOffset>> groupedTopicOffset = group.getGroupedTopicOffset();
            return consumerGroupMapper.fromGroupedTopicOffsetToGroupedTopicOffsetDTO(groupedTopicOffset);
        }

    }

    public List<ConsumerGroupMemberDTO> getConsumerGroupMembers(String clusterId, String consumerGroupId)
            throws ExecutionException, InterruptedException {


        List<ConsumerGroupMemberDTO> consumerGroupMembers = new ArrayList<>();
        List<Consumer> members = this.consumerGroupRepository.findByName(clusterId, consumerGroupId).getMembers();

        members.stream().map(member -> consumerGroupMembers.add(consumerGroupMapper.fromConsumerGroupMemberToConsumerGroupMemberDTO(member))).collect(Collectors.toList());

        return consumerGroupMembers;
    }

//    public List<AclsDTO> getConsumerGroupAcls(String clusterId, String consumerGroupId) {
//        return aclService.getAcls(clusterId, ResourceType.GROUP, consumerGroupId);
//    }

    public void updateConsumerGroupOffsets(ConsumerGroupUpdateDTO consumerGroupUpdateDTO) throws ExecutionException, InterruptedException {
        String clusterId = consumerGroupUpdateDTO.getClusterId();
        String groupId = consumerGroupUpdateDTO.getGroupId();


        ConsumerGroup group = this.consumerGroupRepository.findByName(clusterId, groupId);

        Map<TopicPartition, Long> offsets = group.getOffsets()
                .stream()
                .map(r -> new AbstractMap.SimpleEntry<>(
                        new TopicPartition(r.getTopic(), r.getPartition()),
                        consumerGroupUpdateDTO.getOffsets().get("offset[" + r.getTopic() + "][" + r.getPartition() + "]")
                ))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        this.consumerGroupRepository.updateOffsets(clusterId, groupId, offsets);
    }
}
