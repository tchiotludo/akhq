package org.kafkahq.rest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.kafkahq.models.Topic;
import org.kafkahq.repositories.TopicRepository;
import org.kafkahq.service.TopicService;
import org.kafkahq.service.dto.TopicDTO;
import org.kafkahq.utils.PagedList;
import org.kafkahq.utils.Pagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("${kafkahq.server.base-path:}/api")
public class TopicResource {
    private final Logger log = LoggerFactory.getLogger(TopicResource.class);

    private TopicService topicService;
    private TopicRepository topicRepository;

    @Value("${kafkahq.topic.default-view}")
    private String defaultView;
    @Value("${kafkahq.pagination.page-size}")
    private Integer pageSize;

    @Inject 
    public TopicResource(TopicService topicService){
        this.topicService=topicService;
    }

    @Get("/topicsByType")
    public List<TopicDTO> fetchAllTopicsByType(String topicId, String view) throws ExecutionException, InterruptedException {
        log.debug("Fetch all topics by type");
        return topicService.getAllTopicsByType(topicId, view);
    }

    @Get("/topicsByName")
    public List<TopicDTO> fetchAllTopicsByName(String topicId, String view, String search) throws ExecutionException, InterruptedException {
        log.debug("Fetch all topics by name");
        return topicService.getAllTopicsByName(topicId, view, search);
    }
}