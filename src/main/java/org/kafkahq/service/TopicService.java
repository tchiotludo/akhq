package org.kafkahq.service;

import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import org.apache.kafka.clients.admin.TopicListing;
import org.kafkahq.models.Config;
import org.kafkahq.models.Record;
import org.kafkahq.models.Topic;
import org.kafkahq.modules.AbstractKafkaWrapper;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.modules.RequestHelper;
import org.kafkahq.repositories.RecordRepository;
import org.kafkahq.repositories.TopicRepository;
import org.kafkahq.service.dto.topic.*;
import org.kafkahq.rest.error.ApiError;
import org.kafkahq.service.mapper.TopicMapper;
import org.kafkahq.utils.PagedList;
import org.kafkahq.utils.Pagination;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.*;
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
                        TopicRepository topicRepository, Environment environment, RecordRepository recordRepository) {
        this.kafkaModule = kafkaModule;
        this.topicMapper = topicMapper;
        this.kafkaWrapper = kafkaWrapper;
        this.topicRepository = topicRepository;
        this.environment = environment;
        this.recordRepository = recordRepository;
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

    public void createTopic(CreateTopicDTO createTopicDTO) throws ExecutionException, InterruptedException {
        List<Config> options = new ArrayList<>();
        options.add(new Config("retention.ms", createTopicDTO.getRetention()));
        options.add(new Config("cleanup.policy", createTopicDTO.getCleanupPolicy().toString()));

        topicRepository.create(createTopicDTO.getClusterId(),
                createTopicDTO.getTopicId(),
                createTopicDTO.getPartition(),
                createTopicDTO.getReplicatorFactor(),
                options);
    }

    public void produceTopic (ProduceTopicDTO produceTopicDTO) throws ExecutionException, InterruptedException {

        Map<String, String> finalHeaders = new HashMap<>();
        int i = 0;
        for (String headerKey : produceTopicDTO.getHeaders().get("headers[key]")) {
            if (headerKey != null && !headerKey.equals("") && produceTopicDTO.getHeaders().get("headers[value]").get(i) != null) {
                finalHeaders.put(
                        headerKey,
                        produceTopicDTO.getHeaders().get("headers[value]").get(i).equals("") ? null : produceTopicDTO.getHeaders().get("headers[value]").get(i)
                );
            }
            i++;
        }

        this.recordRepository.produce(
                produceTopicDTO.getClusterId(),
                produceTopicDTO.getTopicId(),
                produceTopicDTO.getValue(),
                finalHeaders,
                produceTopicDTO.getKey().filter(r -> !r.equals("")),
                produceTopicDTO.getPartition(),
                produceTopicDTO.getTimestamp().filter(r -> !r.equals("")).map(r -> Instant.parse(r).toEpochMilli())

        );


    }
}