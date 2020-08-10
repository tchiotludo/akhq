package org.akhq.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.sse.Event;
import io.micronaut.security.annotation.Secured;
import io.swagger.v3.oas.annotations.Operation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.Role;
import org.akhq.models.*;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.repositories.*;
import org.akhq.utils.Pagination;
import org.akhq.utils.ResultNextList;
import org.akhq.utils.ResultPagedList;
import org.akhq.utils.TopicDataResultNextList;
import org.apache.kafka.common.resource.ResourceType;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.reactivestreams.Publisher;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.inject.Inject;

@Slf4j
@Secured(Role.ROLE_TOPIC_READ)
@Controller("${akhq.server.base-path:}/")
public class TopicController extends AbstractController {
    public static final String VALUE_SUFFIX = "-value";
    public static final String KEY_SUFFIX = "-key";
    private final AbstractKafkaWrapper kafkaWrapper;
    private final TopicRepository topicRepository;
    private final ConfigRepository configRepository;
    private final RecordRepository recordRepository;
    private final Environment environment;
    private final AccessControlListRepository aclRepository;

    @Value("${akhq.topic.default-view}")
    private String defaultView;
    @Value("${akhq.topic.replication}")
    private Short replicationFactor;
    @Value("${akhq.topic.retention}")
    private Integer retentionPeriod;
    @Value("${akhq.topic.partition}")
    private Integer partitionCount;
    @Value("${akhq.topic.skip-consumer-groups}")
    protected Boolean skipConsumerGroups;
    @Value("${akhq.pagination.page-size}")
    private Integer pageSize;

    @Inject
    public TopicController(
        AbstractKafkaWrapper kafkaWrapper,
        TopicRepository topicRepository,
        ConfigRepository configRepository,
        RecordRepository recordRepository,
        Environment environment,
        AccessControlListRepository aclRepository,
        SchemaRegistryRepository schemaRegistryRepository
    ) {
        this.kafkaWrapper = kafkaWrapper;
        this.topicRepository = topicRepository;
        this.configRepository = configRepository;
        this.recordRepository = recordRepository;
        this.environment = environment;
        this.aclRepository = aclRepository;
    }

    @Get("api/{cluster}/topic")
    @Operation(tags = {"topic"}, summary = "List all topics")
    public ResultPagedList<Topic> list(
        HttpRequest<?> request,
        String cluster,
        Optional<String> search,
        Optional<TopicRepository.TopicListView> show,
        Optional<Integer> page
    ) throws ExecutionException, InterruptedException {
        URIBuilder uri = URIBuilder.fromURI(request.getUri());
        Pagination pagination = new Pagination(pageSize, uri, page.orElse(1));

        return ResultPagedList.of(this.topicRepository.list(
            cluster,
            pagination,
            show.orElse(TopicRepository.TopicListView.valueOf(defaultView)),
            search
        ));
    }

    @Secured(Role.ROLE_TOPIC_INSERT)
    @Post(value = "api/{cluster}/topic")
    @Operation(tags = {"topic"}, summary = "Create a topic")
    public Topic create(
        String cluster,
        String name,
        Optional<Integer> partition,
        Optional<Short> replication,
        Map<String, String> configs
    ) throws Throwable {
        this.topicRepository.create(
            cluster,
            name,
            partition.orElse(this.partitionCount) ,
            replication.orElse(this.replicationFactor),
            (configs != null ? configs : ImmutableMap.<String, String>of())
                .entrySet()
                .stream()
                .map(r -> new Config(r.getKey(), r.getValue()))
                .collect(Collectors.toList())
        );

        return this.topicRepository.findByName(cluster, name);
    }

    @Secured(Role.ROLE_TOPIC_DATA_INSERT)
    @Post(value = "api/{cluster}/topic/{topicName}/data")
    @Operation(tags = {"topic data"}, summary = "Produce data to a topic")
    public Record produce(
        HttpRequest<?> request,
        String cluster,
        String topicName,
        String value,
        Optional<String> key,
        Optional<Integer> partition,
        Optional<String> timestamp,
        Map<String, String> headers,
        Optional<Integer> keySchema,
        Optional<Integer> valueSchema
    ) throws ExecutionException, InterruptedException {
        return new Record(
            this.recordRepository.produce(
                cluster,
                topicName,
                value,
                headers,
                key,
                partition,
                timestamp.map(r -> Instant.parse(r).toEpochMilli()),
                keySchema,
                valueSchema
            ),
            key.map(String::getBytes).orElse(null),
            value.getBytes(),
            headers
        );
    }

    @Secured(Role.ROLE_TOPIC_DATA_READ)
    @Get("api/{cluster}/topic/{topicName}/data")
    @Operation(tags = {"topic data"}, summary = "Read datas from a topic")
    public ResultNextList<Record> data(
        HttpRequest<?> request,
        String cluster,
        String topicName,
        Optional<String> after,
        Optional<Integer> partition,
        Optional<RecordRepository.Options.Sort> sort,
        Optional<String> timestamp,
        Optional<String> search
    ) throws ExecutionException, InterruptedException {
        Topic topic = this.topicRepository.findByName(cluster, topicName);
        RecordRepository.Options options = dataSearchOptions(cluster, topicName, after, partition, sort, timestamp, search);
        URIBuilder uri = URIBuilder.fromURI(request.getUri());
        List<Record> data = this.recordRepository.consume(cluster, options);

        return TopicDataResultNextList.of(
            data,
            options.after(data, uri),
            (options.getPartition() == null ? topic.getSize() : topic.getSize(options.getPartition())),
            topic.canDeleteRecords(cluster, configRepository)
        );
    }

    @Get("api/{cluster}/topic/{topicName}")
    @Operation(tags = {"topic"}, summary = "Retrieve a topic")
    public Topic home(String cluster, String topicName) throws ExecutionException, InterruptedException {
        return this.topicRepository.findByName(cluster, topicName);
    }

    @Get("api/{cluster}/topic/{topicName}/partitions")
    @Operation(tags = {"topic"}, summary = "List all partition from a topic")
    public List<Partition> partitions(String cluster, String topicName) throws ExecutionException, InterruptedException {
        return this.topicRepository.findByName(cluster, topicName).getPartitions();
    }

    @Get("api/{cluster}/topic/{topicName}/groups")
    @Operation(tags = {"topic"}, summary = "List all consumer groups from a topic")
    public List<ConsumerGroup> groups(String cluster, String topicName) throws ExecutionException, InterruptedException {
        return this.topicRepository.findByName(cluster, topicName, false).getConsumerGroups();
    }

    @Get("api/{cluster}/topic/{topicName}/configs")
    @Operation(tags = {"topic"}, summary = "List all configs from a topic")
    public List<Config> config(String cluster, String topicName) throws ExecutionException, InterruptedException {
        return this.configRepository.findByTopic(cluster, topicName);
    }

    @Get("api/{cluster}/topic/{topicName}/logs")
    @Operation(tags = {"topic"}, summary = "List all logs from a topic")
    public List<LogDir> logs(String cluster, String topicName) throws ExecutionException, InterruptedException {
        return this.topicRepository.findByName(cluster, topicName).getLogDir();
    }

    @Get("api/{cluster}/topic/{topicName}/acls")
    @Operation(tags = {"topic"}, summary = "List all acls from a topic")
    public List<AccessControl> acls(String cluster, String topicName) throws ExecutionException, InterruptedException {
        return aclRepository.findByResourceType(cluster, ResourceType.TOPIC, topicName);
    }

    @Secured(Role.ROLE_TOPIC_CONFIG_UPDATE)
    @Post(value = "api/{cluster}/topic/{topicName}/configs")
    @Operation(tags = {"topic"}, summary = "Update configs from a topic")
    public List<Config> updateConfig(String cluster, String topicName, Map<String, String> configs) throws ExecutionException, InterruptedException {
        List<Config> updated = ConfigRepository.updatedConfigs(configs, this.configRepository.findByTopic(cluster, topicName), false);

        if (updated.size() == 0) {
            throw new IllegalArgumentException("No config to update");
        }

        this.configRepository.updateTopic(
            cluster,
            topicName,
            updated
        );

        return updated;
    }

    @Secured(Role.ROLE_TOPIC_DATA_DELETE)
    @Delete("api/{cluster}/topic/{topicName}/data")
    @Operation(tags = {"topic data"}, summary = "Delete data from a topic by key")
    public Record deleteRecord(String cluster, String topicName, Integer partition, String key) throws ExecutionException, InterruptedException {
        return new Record(
            this.recordRepository.delete(
                cluster,
                topicName,
                partition,
                Base64.getDecoder().decode(key)
            ),
            Base64.getDecoder().decode(key),
            null,
            new HashMap<>()
        );
    }

    @Secured(Role.ROLE_TOPIC_DELETE)
    @Delete("api/{cluster}/topic/{topicName}")
    @Operation(tags = {"topic"}, summary = "Delete a topic")
    public HttpResponse<?> delete(String cluster, String topicName) throws ExecutionException, InterruptedException {
        this.kafkaWrapper.deleteTopics(cluster, topicName);

        return HttpResponse.noContent();
    }

    @Secured(Role.ROLE_TOPIC_DATA_READ)
    @Get("api/{cluster}/topic/{topicName}/data/search/{search}")
    @Operation(tags = {"topic data"}, summary = "Search for data for a topic")
    public Publisher<Event<SearchRecord>> sse(
        String cluster,
        String topicName,
        Optional<String> after,
        Optional<Integer> partition,
        Optional<RecordRepository.Options.Sort> sort,
        Optional<String> timestamp,
        Optional<String> search
    ) throws ExecutionException, InterruptedException {
        Topic topic = topicRepository.findByName(cluster, topicName);

        RecordRepository.Options options = dataSearchOptions(
            cluster,
            topicName,
            after,
            partition,
            sort,
            timestamp,
            search
        );

        return recordRepository
            .search(cluster, options)
            .map(event -> {
                SearchRecord searchRecord = new SearchRecord(
                    event.getData().getPercent(),
                    event.getData().getAfter()
                );

                if (event.getData().getRecords().size() > 0) {
                    searchRecord.records = event.getData().getRecords();
                }

                return Event
                    .of(searchRecord)
                    .name(event.getName());
            });
    }

    private RecordRepository.Options dataSearchOptions(
        String cluster,
        String topicName,
        Optional<String> after,
        Optional<Integer> partition,
        Optional<RecordRepository.Options.Sort> sort,
        Optional<String> timestamp,
        Optional<String> search
    ) {
        RecordRepository.Options options = new RecordRepository.Options(environment, cluster, topicName);

        after.ifPresent(options::setAfter);
        partition.ifPresent(options::setPartition);
        sort.ifPresent(options::setSort);
        timestamp.map(r -> Instant.parse(r).toEpochMilli()).ifPresent(options::setTimestamp);
        after.ifPresent(options::setAfter);
        search.ifPresent(options::setSearch);

        return options;
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    public static class SearchRecord {
        public SearchRecord(double percent, String after) {
            this.percent = percent;
            this.after = after;
        }

        @JsonProperty("percent")
        private final Double percent;

        @JsonProperty("records")
        private List<Record> records;

        @JsonProperty("after")
        private final String after;
    }
}
