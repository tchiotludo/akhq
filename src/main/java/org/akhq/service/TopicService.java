package org.akhq.service;

import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import org.apache.commons.lang3.tuple.Pair;
import org.akhq.models.Config;
import org.akhq.models.ConsumerGroup;
import org.akhq.models.LogDir;
import org.akhq.models.Record;
import org.akhq.models.Topic;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.modules.KafkaModule;
import org.akhq.repositories.ConfigRepository;
import org.akhq.repositories.LogDirRepository;
import org.akhq.repositories.RecordRepository;
import org.akhq.repositories.TopicRepository;
import org.akhq.service.dto.consumerGroup.ConsumerGroupDTO;
import org.akhq.service.dto.topic.ConfigOperationDTO;
import org.akhq.service.dto.topic.ConfigDTO;
import org.akhq.service.dto.topic.CreateTopicDTO;
import org.akhq.service.dto.topic.LogDTO;
import org.akhq.service.dto.topic.PartitionDTO;
import org.akhq.service.dto.topic.ProduceTopicDTO;
import org.akhq.service.dto.topic.RecordDTO;
import org.akhq.service.dto.topic.TopicDTO;
import org.akhq.service.dto.topic.TopicDataDTO;
import org.akhq.service.dto.topic.TopicListDTO;
import org.akhq.service.mapper.ConsumerGroupMapper;
import org.akhq.service.mapper.TopicMapper;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class TopicService {
    private KafkaModule kafkaModule;
    private AbstractKafkaWrapper kafkaWrapper;
    private Environment environment;
    private ConsumerGroupMapper consumerGroupMapper;
    private TopicRepository topicRepository;
    private RecordRepository recordRepository;
    private LogDirRepository logDirRepository;
    private ConfigRepository configRepository;

    private TopicMapper topicMapper;

    @Value("${akhq.topic.default-view}")
    private String defaultView;
    @Value("${akhq.pagination.page-size}")
    private Integer pageSize;

    @Inject
    public TopicService(KafkaModule kafkaModule,ConsumerGroupMapper consumerGroupMapper, TopicMapper topicMapper, AbstractKafkaWrapper kafkaWrapper,
                        TopicRepository topicRepository, Environment environment, RecordRepository recordRepository, ConfigRepository configRepository) {
        this.kafkaModule = kafkaModule;
        this.consumerGroupMapper=consumerGroupMapper;
        this.topicMapper = topicMapper;
        this.kafkaWrapper = kafkaWrapper;
        this.topicRepository = topicRepository;
        this.environment = environment;
        this.recordRepository = recordRepository;
        this.configRepository = configRepository;
    }

    public TopicListDTO getTopics(String clusterId, String view, String search, Optional<Integer> pageNumber)
            throws ExecutionException, InterruptedException {
        TopicRepository.TopicListView topicListView = TopicRepository.TopicListView.valueOf(view);

        Pagination pagination = new Pagination(pageSize, pageNumber.orElse(1));
        PagedList<Topic> pagedList = this.topicRepository.list(
                clusterId,
                pagination,
                topicListView,
                Optional.ofNullable(search)
        );

        List<TopicDTO> topicDTOList = new ArrayList<>();
        pagedList
                .stream()
                .map(topic -> topicDTOList.add(topicMapper.fromTopicToTopicDTO(topic))).collect(Collectors.toList());

        return new TopicListDTO(topicDTOList,pagedList.pageCount());
    }

    public TopicDataDTO getTopicData(String clusterId, String topicId,
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

        List<Record> data;

        if (options.getSearch() == null) {
            data = this.recordRepository.consume(clusterId, options);
        } else {
            data = recordRepository
                    .search(clusterId, options)
                    .map(event -> {
                                if (event.getData().getRecords().size() > 0) {
                                    return event.getData().getRecords();
                                }
                                return new ArrayList<Record>();
                            }
                    ).blockingFirst();
        }

        Pair<Long, Integer> dataAndPageCount = getTopicDataAndPageCount(clusterId, topicId, options);
        String afterString = options.pagination(data);

        Optional<List<RecordDTO>> messageList = Optional.of(data.stream().map(record -> topicMapper.fromRecordToRecordDTO(record)).collect(Collectors.toList()));

        return new TopicDataDTO(
                messageList.orElse(new ArrayList<>()),
                afterString,
                dataAndPageCount.getLeft()
//                dataAndPageCount.getRight() //To be added when a way to correctly paginate topic data is found
        );
    }

    private Pair<Long, Integer> getTopicDataAndPageCount(String clusterId, String topicId, RecordRepository.Options options) throws ExecutionException, InterruptedException {
        Topic topic = this.topicRepository.findByName(clusterId, topicId);
        long recordCount = (options.getPartition() == null ? topic.getSize() : topic.getSize(options.getPartition()));
        return Pair.of(recordCount, recordCount > 0 ? (int) Math.ceil((double) (recordCount / pageSize)) : 1);
    }

    public List<PartitionDTO> getTopicPartitions(String clusterId, String topicId) throws ExecutionException, InterruptedException {
        Topic topic = this.topicRepository.findByName(clusterId, topicId);

        return topic.getPartitions().stream().map(partition -> topicMapper.fromPartitionToPartitionDTO(partition))
                .collect(Collectors.toList());
    }

    public List<LogDTO> getTopicLogs(String clusterId, String topicId) throws ExecutionException, InterruptedException {
        Topic topic = this.topicRepository.findByName(clusterId, topicId);

        return topic.getLogDir().stream().map(log -> topicMapper.fromLogToLogDTO(log))
                .collect(Collectors.toList());
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

    public void deleteTopic(String clusterId, String topicId) throws ExecutionException, InterruptedException {
        topicRepository.delete(clusterId, topicId);
    }

    public void produceToTopic(ProduceTopicDTO produceTopicDTO) throws ExecutionException, InterruptedException {
        this.recordRepository.produce(
                produceTopicDTO.getClusterId(),
                produceTopicDTO.getTopicId(),
                produceTopicDTO.getValue(),
                produceTopicDTO.getHeaders(),
                Optional.of(produceTopicDTO.getKey()).filter(r -> !r.equals("")),
                Optional.ofNullable(produceTopicDTO.getPartition()),
                Optional.ofNullable(produceTopicDTO.getTimestamp()).filter(r -> !r.equals("")).map(r -> Instant.parse(r).toEpochMilli())
        );
    }

    public List<ConsumerGroupDTO>  getConsumerGroups(String clusterId, String topicName)
            throws ExecutionException, InterruptedException {


        List<ConsumerGroup> list =  this.topicRepository.findByName(clusterId,topicName).getConsumerGroups();


        List<ConsumerGroupDTO> consumerGroupList = new ArrayList<>();
        list.stream().map(consumerGroup -> consumerGroupList.add(consumerGroupMapper.fromConsumerGroupToConsumerGroupDTO(consumerGroup))).collect(Collectors.toList());

        return  consumerGroupList;
    }

    public List<LogDTO> getLogDTOList(String clusterId, Integer nodeId) throws ExecutionException, InterruptedException {
        List<LogDir> logList = logDirRepository.findByBroker(clusterId, nodeId);

        return logList.stream().map(logDir -> topicMapper.fromLogToLogDTO(logDir)).collect(Collectors.toList());
    }

    public List<ConfigDTO> getConfigDTOList(String clusterId, String topicId) throws ExecutionException, InterruptedException {

        List<Config> configList = this.configRepository.findByTopic(clusterId, topicId);

        configList.sort((o1, o2) -> o1.isReadOnly() == o2.isReadOnly() ? 0 :
                (o1.isReadOnly() ? 1 : -1)
        );

        return configList.stream().map(config -> topicMapper.fromConfigToConfigDTO(config)).collect(Collectors.toList());
    }

    public List<ConfigDTO> updateConfigs(ConfigOperationDTO configOperation) throws Throwable {
        Map<String, String> configs = topicMapper.convertConfigsMap(configOperation.getConfigs());
        List<Config> updated = ConfigRepository.updatedConfigs(configs, this.configRepository.findByTopic(configOperation.getClusterId(), configOperation.getTopicId()));

        if (updated.size() == 0) {
            throw new IllegalArgumentException("No config to update");
        }

        this.configRepository.updateTopic(
                configOperation.getClusterId(),
                configOperation.getTopicId(),
                updated
        );

        return updated.stream()
                .map(config -> topicMapper.fromConfigToConfigDTO(config))
                .collect(Collectors.toList());
    }

}
