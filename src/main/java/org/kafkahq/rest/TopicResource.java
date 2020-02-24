package org.kafkahq.rest;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.annotation.Body;
import org.kafkahq.repositories.TopicRepository;
import org.kafkahq.service.TopicService;
import org.kafkahq.service.dto.TopicDTO;
import org.kafkahq.service.dto.topic.CreateTopicDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;

@Controller("${kafkahq.server.base-path:}/api")
public class TopicResource {
    private final Logger log = LoggerFactory.getLogger(TopicResource.class);

    private TopicService topicService;

    @Inject 
    public TopicResource(TopicService topicService){
        this.topicService=topicService;
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
}

