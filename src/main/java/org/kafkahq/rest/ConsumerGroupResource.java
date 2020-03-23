package org.kafkahq.rest;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import lombok.extern.slf4j.Slf4j;
import org.kafkahq.service.ConsumerGroupService;
import org.kafkahq.service.dto.consumerGroup.ConsumerGroupMemberDTO;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.kafkahq.service.dto.consumerGroup.ConsumerGroupListDTO;
import org.kafkahq.service.dto.consumerGroup.ConsumerGroupOffsetDTO;
import org.kafkahq.service.dto.consumerGroup.ConsumerGroupUpdateDTO;
import org.kafkahq.service.dto.consumerGroup.GroupedTopicOffsetDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Slf4j
@Controller("${kafkahq.server.base-path:}/api")
public class ConsumerGroupResource {
    private ConsumerGroupService consumerGroupService;

    @Inject
    public ConsumerGroupResource(ConsumerGroupService consumerGroupService) {
        this.consumerGroupService = consumerGroupService;
    }

    @Get("/group")
    public ConsumerGroupListDTO fetchAllConsumerGroup(String clusterId, @Nullable String search, Optional<Integer> pageNumber) throws ExecutionException, InterruptedException {
        log.debug("Fetch all Consumer Groups");
        return consumerGroupService.getConsumerGroup(clusterId, Optional.ofNullable(search), pageNumber);
    }

    @Get("/group/topics")
    public List<ConsumerGroupOffsetDTO> fetchConsumerGroupOffsets(String clusterId, String groupId) throws ExecutionException, InterruptedException {
        log.debug("Fetch data from topic: {}", groupId);
        return consumerGroupService.getConsumerGroupOffsets(clusterId, groupId);
    }

    @Get("/group/members")
    public List<ConsumerGroupMemberDTO> fetchAllConsumerGroupMembers(String clusterId, String groupId) throws ExecutionException, InterruptedException {
        log.debug("Fetch consumer group members from: {}", groupId);
        return consumerGroupService.getConsumerGroupMembers(clusterId, groupId);
    }

    @Get("/group/grouped-topic-offset")
    public GroupedTopicOffsetDTO fetchGroupedTopicOffset(String clusterId, String groupId, Optional<String> timestamp) throws ExecutionException, InterruptedException {
        log.debug("Fetch grouped topic offset from: {}", groupId);
        return consumerGroupService.getConsumerGroupGroupedTopicOffsets(clusterId, groupId, timestamp);
    }

    @Post("/group/update")
    public void updateConsumerGroupOffsets(@Body ConsumerGroupUpdateDTO consumerGroupUpdateDTO) throws ExecutionException, InterruptedException {
        log.debug("Updating consumer group offsets: {}", consumerGroupUpdateDTO.getGroupId());
        consumerGroupService.updateConsumerGroupOffsets(consumerGroupUpdateDTO);
    }
}


