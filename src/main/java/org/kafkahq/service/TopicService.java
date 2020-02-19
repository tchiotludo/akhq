package org.kafkahq.service;

import io.micronaut.context.annotation.Value;
import org.apache.kafka.clients.admin.TopicListing;
import org.kafkahq.models.Topic;
import org.kafkahq.modules.AbstractKafkaWrapper;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.repositories.TopicRepository;
import org.kafkahq.service.dto.TopicDTO;
import org.kafkahq.service.mapper.TopicMapper;
import org.kafkahq.utils.PagedList;
import org.kafkahq.utils.Pagination;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutionException;

@Singleton
public class TopicService {
    private KafkaModule kafkaModule;
    private TopicRepository topicRepository;
    private TopicMapper topicMapper;
    private AbstractKafkaWrapper kafkaWrapper;

    @Value("${kafkahq.topic.default-view}")
    private String defaultView;
    @Value("${kafkahq.pagination.page-size}")
    private Integer pageSize;

    @Inject
    public TopicService(KafkaModule kafkaModule, TopicMapper topicMapper, AbstractKafkaWrapper kafkaWrapper, TopicRepository topicRepository) {
        this.kafkaModule = kafkaModule;
        this.topicMapper = topicMapper;
        this.kafkaWrapper=kafkaWrapper;
        this.topicRepository=topicRepository;
    }

    public List<TopicDTO> getAllTopics(String clusterId) throws ExecutionException, InterruptedException {
        //return kafkaModule.getTopicsList().stream().map(TopicDTO::new).collect(Collectors.toList());
        Collection<TopicListing> listTopics = kafkaWrapper.listTopics(clusterId);

        List<Topic> topicList = listTopics.stream().map(topicListing -> {
            try {
                return topicRepository.findByName(clusterId, topicListing.name());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());

        return topicList.stream().map(topic -> topicMapper.fromTopicToTopicDTO(topic)).collect(Collectors.toList());
    }

    public List<TopicDTO> getAll(String clusterId, String view, String search) throws ExecutionException, InterruptedException {
        TopicRepository.TopicListView topicListView = TopicRepository.TopicListView.valueOf(view);
        Pagination pagination = new Pagination(pageSize, 1);
        PagedList<Topic> pagedList = this.topicRepository.list(
                clusterId,
                pagination,
                topicListView,
                Optional.ofNullable(search)
        );

        List<TopicDTO> topicDTOList = new ArrayList<>();
        pagedList
                .stream()
                .map(topicDTOItem -> topicDTOList.add(topicMapper.fromTopicToTopicDTO(topicDTOItem))).collect(Collectors.toList());

        return topicDTOList;
    }


    public List<TopicDTO> getAllTopicsByName(String clusterId, String view, String search)
       throws ExecutionException, InterruptedException {
        return getAll(clusterId, view, search);
    }

    public List<TopicDTO> getAllTopicsByType(String clusterId, String view) throws ExecutionException, InterruptedException {
        /*TopicRepository.TopicListView topicListView = TopicRepository.TopicListView.valueOf(view);

       URIBuilder uri = URIBuilder.fromURI(request.getUri());
       Pagination pagination = new Pagination(pageSize, 1);*/
       return getAll(clusterId, view, "");
    }
}