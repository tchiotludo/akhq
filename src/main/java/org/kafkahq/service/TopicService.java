package org.kafkahq.service;

import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import org.apache.kafka.clients.admin.TopicListing;
import org.kafkahq.models.Record;
import org.kafkahq.models.Topic;
import org.kafkahq.modules.AbstractKafkaWrapper;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.repositories.RecordRepository;
import org.kafkahq.repositories.TopicRepository;
import org.kafkahq.rest.error.ApiError;
import org.kafkahq.service.dto.topic.PartitionDTO;
import org.kafkahq.service.dto.topic.RecordDTO;
import org.kafkahq.service.dto.topic.TopicDTO;
import org.kafkahq.service.mapper.TopicMapper;
import org.kafkahq.utils.PagedList;
import org.kafkahq.utils.Pagination;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutionException;

@Singleton
public class TopicService {
    private KafkaModule kafkaModule;
    private AbstractKafkaWrapper kafkaWrapper;
    private Environment environment;

    private TopicRepository topicRepository;
    private RecordRepository recordRepository;

    private TopicMapper topicMapper;

    @Value("${kafkahq.topic.default-view}")
    private String defaultView;
    @Value("${kafkahq.pagination.page-size}")
    private Integer pageSize;

    @Inject
    public TopicService(KafkaModule kafkaModule, TopicMapper topicMapper, AbstractKafkaWrapper kafkaWrapper,
                        TopicRepository topicRepository, Environment environment) {
        this.kafkaModule = kafkaModule;
        this.topicMapper = topicMapper;
        this.kafkaWrapper = kafkaWrapper;
        this.topicRepository = topicRepository;
        this.environment = environment;
    }

    public List<TopicDTO> getAllTopicsByName(String clusterId, String view, String search)
            throws ExecutionException, InterruptedException {
        return getAll(clusterId, view, search);
    }

    public List<TopicDTO> getAllTopicsByType(String clusterId, String view) throws ExecutionException, InterruptedException {
        return getAll(clusterId, view, "");
    }

    public List<RecordDTO> getTopicData(String clusterId, String topicId,
                                        Optional<String> after,
                                        Optional<Integer> partition,
                                        Optional<RecordRepository.Options.Sort> sort,
                                        Optional<String> timestamp,
                                        Optional<String> search) throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, clusterId, topicId);
        after.ifPresent(options::setAfter);
        partition.ifPresent(options::setPartition);
        sort.ifPresent(options::setSort);
        timestamp.map(r -> Instant.parse(r).toEpochMilli()).ifPresent(options::setTimestamp);
        after.ifPresent(options::setAfter);
        search.ifPresent(options::setSearch);

        List<Record> data = new ArrayList<>();

        if (options.getSearch() == null) {
            data = this.recordRepository.consume(clusterId, options);
        }

        return data.stream().map(record -> topicMapper.fromRecordToRecordDTO(record)).collect(Collectors.toList());
    }

    public List<PartitionDTO> getTopicPartitions(String clusterId, String topicId) throws ExecutionException, InterruptedException {
        Topic topic = this.topicRepository.findByName(clusterId, topicId);

        return topic.getPartitions().stream().map(partition -> topicMapper.fromPartitionToPartitionDTO(partition))
                .collect(Collectors.toList());
    }

    private List<TopicDTO> getAllTopics(String clusterId) throws ExecutionException, InterruptedException {
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

    private List<TopicDTO> getAll(String clusterId, String view, String search) throws ExecutionException, InterruptedException {
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
}
