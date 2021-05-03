package org.akhq.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.sse.Event;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.swagger.v3.oas.annotations.Operation;
import lombok.*;
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
import org.akhq.models.Record;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.akhq.models.Record;

@Slf4j
@Secured(Role.ROLE_TOPIC_READ)
@Controller
public class TopicController extends AbstractController {
    public static final String VALUE_SUFFIX = "-value";
    public static final String KEY_SUFFIX = "-key";
    @Inject
    private AbstractKafkaWrapper kafkaWrapper;
    @Inject
    private TopicRepository topicRepository;
    @Inject
    private ConfigRepository configRepository;
    @Inject
    private RecordRepository recordRepository;
    @Inject
    private ConsumerGroupRepository consumerGroupRepository;
    @Inject
    private Environment environment;
    @Inject
    private AccessControlListRepository aclRepository;
    @Inject
    private SchemaRegistryRepository schemaRegistryRepository;

    @Value("${akhq.topic.replication}")
    private Short replicationFactor;
    @Value("${akhq.topic.partition}")
    private Integer partitionCount;
    @Value("${akhq.pagination.page-size}")
    private Integer pageSize;

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
            show.orElse(TopicRepository.TopicListView.HIDE_INTERNAL),
            search
        ));
    }

    @Get("api/{cluster}/topic/name")
    @Operation(tags = {"topic"}, summary = "List all topics name")
    public List<String> listTopicNames(
            HttpRequest<?> request,
            String cluster,
            Optional<TopicRepository.TopicListView> show
    ) throws ExecutionException, InterruptedException {
        return this.topicRepository.all(cluster, show.orElse(TopicRepository.TopicListView.HIDE_INTERNAL), Optional.empty());
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
            schemaRegistryRepository.getSchemaRegistryType(cluster),
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
        Optional<String> searchByKey,
        Optional<String> searchByValue,
        Optional<String> searchByHeaderKey,
        Optional<String> searchByHeaderValue
    ) throws ExecutionException, InterruptedException {
        Topic topic = this.topicRepository.findByName(cluster, topicName);
        RecordRepository.Options options =
                dataSearchOptions(cluster,
                        topicName,
                        after,
                        partition,
                        sort,
                        timestamp,
                        searchByKey,
                        searchByValue,
                        searchByHeaderKey,
                        searchByHeaderValue);
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

    @Get("api/{cluster}/topic/last-record")
    @Operation(tags = {"topic"}, summary = "Retrieve the last record for a list of topics")
    public Map<String, Record> lastRecord(String cluster, List<String> topics) throws ExecutionException, InterruptedException {
        return this.recordRepository.getLastRecord(cluster, topics);
    }

    @Get("api/{cluster}/topic/{topicName}/partitions")
    @Operation(tags = {"topic"}, summary = "List all partition from a topic")
    public List<Partition> partitions(String cluster, String topicName) throws ExecutionException, InterruptedException {
        return this.topicRepository.findByName(cluster, topicName).getPartitions();
    }

    @Get("api/{cluster}/topic/{topicName}/groups")
    @Operation(tags = {"topic"}, summary = "List all consumer groups from a topic")
    public List<ConsumerGroup> groups(String cluster, String topicName) throws ExecutionException, InterruptedException {
        return this.consumerGroupRepository.findByTopic(cluster, topicName);
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
    @Delete("api/{cluster}/topic/{topicName}/data/empty")
    @Operation(tags = {"topic data"}, summary = "Empty data from a topic")
    public HttpResponse<?> emptyTopic(String cluster, String topicName) throws ExecutionException, InterruptedException{
        this.recordRepository.emptyTopic(
                cluster,
                topicName
        );

        return HttpResponse.noContent();
    }

    @Secured(Role.ROLE_TOPIC_DATA_DELETE)
    @Delete("api/{cluster}/topic/{topicName}/data")
    @Operation(tags = {"topic data"}, summary = "Delete data from a topic by key")
    public Record deleteRecordApi(String cluster, String topicName, Integer partition, String key) throws ExecutionException, InterruptedException {
        return new Record(
            this.recordRepository.delete(
                cluster,
                topicName,
                partition,
                Base64.getDecoder().decode(key)
            ),
            schemaRegistryRepository.getSchemaRegistryType(cluster),
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
    @ExecuteOn(TaskExecutors.IO)
    @Get(value = "api/{cluster}/topic/{topicName}/data/search", produces = MediaType.TEXT_EVENT_STREAM)
    @Operation(tags = {"topic data"}, summary = "Search for data for a topic")
    public Publisher<Event<SearchRecord>> sse(
        String cluster,
        String topicName,
        Optional<String> after,
        Optional<Integer> partition,
        Optional<RecordRepository.Options.Sort> sort,
        Optional<String> timestamp,
        Optional<String> searchByKey,
        Optional<String> searchByValue,
        Optional<String> searchByHeaderKey,
        Optional<String> searchByHeaderValue
    ) throws ExecutionException, InterruptedException {
        RecordRepository.Options options = dataSearchOptions(
            cluster,
            topicName,
            after,
            partition,
            sort,
            timestamp,
            searchByKey,
            searchByValue,
            searchByHeaderKey,
            searchByHeaderValue
        );

        Topic topic = topicRepository.findByName(cluster, topicName);

        return recordRepository
            .search(topic, options)
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

    @Secured(Role.ROLE_TOPIC_DATA_READ)
    @Get("api/{cluster}/topic/{topicName}/data/record/{partition}/{offset}")
    @Operation(tags = {"topic data"}, summary = "Get a single record by partition and offset")
    public ResultNextList<Record> record(
            HttpRequest<?> request,
            String cluster,
            String topicName,
            Integer partition,
            Integer offset
    ) throws ExecutionException, InterruptedException {
        Topic topic = this.topicRepository.findByName(cluster, topicName);

        // after wait for next offset, so add - 1 to allow to have the current offset
        RecordRepository.Options options = dataSearchOptions(
            cluster,
            topicName,
            offset - 1 < 0 ? Optional.empty() : Optional.of(String.join("-", String.valueOf(partition), String.valueOf(offset - 1))),
            Optional.of(partition),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );

        Optional<Record> singleRecord = this.recordRepository.consumeSingleRecord(cluster, topic, options);
        List<Record> data = singleRecord.map(Collections::singletonList).orElse(Collections.emptyList());

        return TopicDataResultNextList.of(
                data,
                URIBuilder.empty(),
                data.size(),
                topic.canDeleteRecords(cluster, configRepository)
        );
    }

    @Secured(Role.ROLE_TOPIC_DATA_READ)
    @Get("api/{cluster}/topic/{topicName}/offsets/start")
    @Operation(tags = {"topic data"}, summary = "Get topic partition offsets by timestamp")
    public List<RecordRepository.TimeOffset> offsetsStart(String cluster, String topicName, Optional<Instant> timestamp) throws ExecutionException, InterruptedException {
        Topic topic = this.topicRepository.findByName(cluster, topicName);

        return recordRepository.getOffsetForTime(
                cluster,
                topic.getPartitions()
                        .stream()
                        .map(r -> new TopicPartition(r.getTopic(), r.getId()))
                        .collect(Collectors.toList()),
                timestamp.orElse(Instant.now()).toEpochMilli()
        );
    }


    @Secured(Role.ROLE_TOPIC_DATA_INSERT)
    @Post("api/{fromCluster}/topic/{fromTopicName}/copy/{toCluster}/topic/{toTopicName}")
    @Operation(tags = {"topic data"}, summary = "Copy from a topic to another topic")
    public RecordRepository.CopyResult copy(
            HttpRequest<?> request,
            String fromCluster,
            String fromTopicName,
            String toCluster,
            String toTopicName,
            @Body List<OffsetCopy> offsets
    ) throws ExecutionException, InterruptedException {
        Topic fromTopic = this.topicRepository.findByName(fromCluster, fromTopicName);
        Topic toTopic = this.topicRepository.findByName(toCluster, toTopicName);

        if (!CollectionUtils.isNotEmpty(offsets)) {
            throw new IllegalArgumentException("Empty collections");
        }

        // after wait for next offset, so add - 1 to allow to have the current offset
        String offsetsList = offsets.stream()
            .filter(offsetCopy -> offsetCopy.offset - 1 >= 0)
            .map(offsetCopy ->
                String.join("-", String.valueOf(offsetCopy.partition), String.valueOf(offsetCopy.offset - 1)))
            .collect(Collectors.joining("_"));

        RecordRepository.Options options = dataSearchOptions(
            fromCluster,
            fromTopicName,
            Optional.ofNullable(StringUtils.isNotEmpty(offsetsList) ? offsetsList : null),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );

        return this.recordRepository.copy(fromTopic, toCluster, toTopic, offsets, options);
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    public static class CopyResponse {
        int records;
    }

    private RecordRepository.Options dataSearchOptions(
        String cluster,
        String topicName,
        Optional<String> after,
        Optional<Integer> partition,
        Optional<RecordRepository.Options.Sort> sort,
        Optional<String> timestamp,
        Optional<String> searchByKey,
        Optional<String> searchByValue,
        Optional<String> searchByHeaderKey,
        Optional<String> searchByHeaderValue
    ) {
        RecordRepository.Options options = new RecordRepository.Options(environment, cluster, topicName);

        after.ifPresent(options::setAfter);
        partition.ifPresent(options::setPartition);
        sort.ifPresent(options::setSort);
        timestamp.map(r -> Instant.parse(r).toEpochMilli()).ifPresent(options::setTimestamp);
        after.ifPresent(options::setAfter);
        searchByKey.ifPresent(options::setSearchByKey);
        searchByValue.ifPresent(options::setSearchByValue);
        searchByHeaderKey.ifPresent(options::setSearchByHeaderKey);
        searchByHeaderValue.ifPresent(options::setSearchByHeaderValue);
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

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class OffsetCopy {
        private int partition;
        private long offset;
    }
}
