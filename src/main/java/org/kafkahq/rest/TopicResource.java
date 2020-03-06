package org.kafkahq.rest;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.annotation.Body;
import org.kafkahq.repositories.TopicRepository;

import org.kafkahq.service.dto.topic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;

import lombok.extern.slf4j.Slf4j;
import org.kafkahq.repositories.RecordRepository;
import org.kafkahq.service.TopicService;


import javax.validation.constraints.Null;

import java.util.Optional;

@Slf4j
@Controller("${kafkahq.server.base-path:}/api")
public class TopicResource {
    private TopicService topicService;

    @Inject
    public TopicResource(TopicService topicService) {
        this.topicService = topicService;
    }

    @Get("/topicsByType")
    public List<TopicDTO> fetchAllTopicsByType(String clusterId, String view) throws ExecutionException, InterruptedException {
        log.debug("Fetch all topics by type");
        return topicService.getAllTopicsByType(clusterId, view);
    }

    @Get("/topicsByName")
    public List<TopicDTO> fetchAllTopicsByName(String clusterId, String view, String search) throws ExecutionException, InterruptedException {
        log.debug("Fetch all topics by name");
        return topicService.getAllTopicsByName(clusterId, view, search);
    }

    @Post("/topic/create")
    public void topicCreate(@Body CreateTopicDTO createTopicDTO) throws ExecutionException, InterruptedException {
        log.debug("Create Topic {}", createTopicDTO);
        topicService.createTopic(createTopicDTO);
    }

    @Get("/topic/data")
    public List<RecordDTO> fetchTopicData(String clusterId, String topicId,
                                          Optional<String> after,
                                          Optional<Integer> partition,
                                          Optional<RecordRepository.Options.Sort> sort,
                                          Optional<String> timestamp,
                                          Optional<String> search) throws ExecutionException, InterruptedException {
        log.debug("Fetch data from topic: {}", topicId);
        return topicService.getTopicData(clusterId, topicId, after, partition, sort, timestamp, search);
    }

    @Get("/topic/partitions")
    public List<PartitionDTO> fetchTopicPartitions(String clusterId, String topicId) throws ExecutionException, InterruptedException {
        log.debug("Fetch partitions from topic: {}", topicId);
        return topicService.getTopicPartitions(clusterId, topicId);
    }
}

