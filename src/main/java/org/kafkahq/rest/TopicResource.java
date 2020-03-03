package org.kafkahq.rest;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import lombok.extern.slf4j.Slf4j;
import org.kafkahq.repositories.RecordRepository;
import org.kafkahq.service.TopicService;
import org.kafkahq.service.dto.topic.CreateTopicDTO;
import org.kafkahq.service.dto.topic.PartitionDTO;
import org.kafkahq.service.dto.topic.RecordDTO;
import org.kafkahq.service.dto.topic.TopicDTO;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Slf4j
@Controller("${kafkahq.server.base-path:}/api")
public class TopicResource {
    private TopicService topicService;

    @Inject
    public TopicResource(TopicService topicService) {
        this.topicService = topicService;
    }

    @Get("/topics")
    public List<TopicDTO> fetchAllTopics(String clusterId, String view, @Nullable String search) throws ExecutionException, InterruptedException {
        log.debug("Fetch all topics by name");
        return topicService.getTopics(clusterId, view, search);
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

