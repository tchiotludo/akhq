package org.akhq.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableMap;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.types.files.StreamedFile;
import io.micronaut.http.sse.Event;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.reactivex.schedulers.Schedulers;
import io.swagger.v3.oas.annotations.Operation;
import lombok.*;
import org.akhq.configs.security.Role;
import org.akhq.models.*;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.repositories.*;
import org.akhq.security.annotation.AKHQSecured;
import org.akhq.utils.Pagination;
import org.akhq.utils.ResultNextList;
import org.akhq.utils.ResultPagedList;
import org.akhq.utils.TopicDataResultNextList;
import org.apache.kafka.common.resource.ResourceType;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.reactivestreams.Publisher;
import org.akhq.models.Record;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

@Secured(SecurityRule.IS_AUTHENTICATED)
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
    @Value("${akhq.topic.retention}")
    private Integer retention;
    @Value("${akhq.pagination.page-size}")
    private Integer pageSize;

    @Get ("api/topic/defaults-configs")
    @Operation(tags = {"topic"}, summary = "Get default topic configuration")
    public Map<String,Integer> getDefaultConf(){
        return Map.of(
            "replication", replicationFactor.intValue(),
            "partition", partitionCount,
            "retention", retention
        );
    }

    @Get("api/{cluster}/topic")
    @AKHQSecured(resource = Role.Resource.TOPIC, action = Role.Action.READ)
    @Operation(tags = {"topic"}, summary = "List all topics")
    public ResultPagedList<Topic> list(
        HttpRequest<?> request,
        String cluster,
        Optional<String> search,
        Optional<TopicRepository.TopicListView> show,
        Optional<Integer> page,
        Optional<Integer> uiPageSize
    ) throws ExecutionException, InterruptedException {
        checkIfClusterAllowed(cluster);

        URIBuilder uri = URIBuilder.fromURI(request.getUri());
        Pagination pagination = new Pagination(uiPageSize.orElse(pageSize), uri, page.orElse(1));

        return ResultPagedList.of(this.topicRepository.list(
            cluster,
            pagination,
            show.orElse(TopicRepository.TopicListView.HIDE_INTERNAL),
            search,
            buildUserBasedResourceFilters(cluster)
        ));
    }

    @AKHQSecured(resource = Role.Resource.TOPIC, action = Role.Action.READ)
    @Get("api/{cluster}/topic/name")
    @Operation(tags = {"topic"}, summary = "List all topics name")
    public List<String> listTopicNames(
            HttpRequest<?> request,
            String cluster,
            Optional<TopicRepository.TopicListView> show
    ) throws ExecutionException, InterruptedException {
        checkIfClusterAllowed(cluster);

        return this.topicRepository.all(cluster,
            show.orElse(TopicRepository.TopicListView.HIDE_INTERNAL),
            Optional.empty(),
            buildUserBasedResourceFilters(cluster));
    }


    @AKHQSecured(resource = Role.Resource.TOPIC, action = Role.Action.CREATE)
    @Post(value = "api/{cluster}/topic")
    @Operation(tags = {"topic"}, summary = "Create a topic")
    public Topic create(
        String cluster,
        String name,
        Optional<Integer> partition,
        Optional<Short> replication,
        Map<String, String> configs
    ) throws Throwable {
        checkIfClusterAndResourceAllowed(cluster, name);

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

    @AKHQSecured(resource = Role.Resource.TOPIC_DATA, action = Role.Action.CREATE)
    @Post(value = "api/{cluster}/topic/{topicName}/data")
    @Operation(tags = {"topic data"}, summary = "Produce data to a topic")
    public List<Record> produce(
        HttpRequest<?> request,
        String cluster,
        String topicName,
        Optional<String> value,
        Optional<String> key,
        Optional<Integer> partition,
        Optional<String> timestamp,
        List<KeyValue<String, String>> headers,
        Optional<String> keySchema,
        Optional<String> valueSchema,
        Boolean multiMessage,
        Optional<String> keyValueSeparator
    ) throws ExecutionException, InterruptedException, RestClientException, IOException {
        checkIfClusterAndResourceAllowed(cluster, topicName);

        Topic targetTopic = topicRepository.findByName(cluster, topicName);
        return
            this.recordRepository.produce(
                cluster,
                topicName,
                value,
                headers,
                key,
                partition,
                timestamp.map(r -> Instant.parse(r).toEpochMilli()),
                keySchema,
                valueSchema,
                multiMessage,
                keyValueSeparator).stream()
                    .map(recordMetadata -> new Record(recordMetadata,
                            schemaRegistryRepository.getSchemaRegistryType(cluster),
                            key.map(String::getBytes).orElse(null),
                            value.map(String::getBytes).orElse(null),
                            headers,
                            targetTopic, null))
                    .collect(Collectors.toList());
    }

    @AKHQSecured(resource = Role.Resource.TOPIC_DATA, action = Role.Action.READ)
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
        Optional<String> endTimestamp,
        Optional<String> searchByKey,
        Optional<String> searchByValue,
        Optional<String> searchByHeaderKey,
        Optional<String> searchByHeaderValue,
        Optional<String> searchByKeySubject,
        Optional<String> searchByValueSubject
    ) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, topicName);

        Topic topic = this.topicRepository.findByName(cluster, topicName);
        RecordRepository.Options options =
                dataSearchOptions(cluster,
                        topicName,
                        after,
                        partition,
                        sort,
                        timestamp,
                        endTimestamp,
                        searchByKey,
                        searchByValue,
                        searchByHeaderKey,
                        searchByHeaderValue,
                        searchByKeySubject,
                        searchByValueSubject);
        URIBuilder uri = URIBuilder.fromURI(request.getUri());
        List<Record> data = this.recordRepository.consume(cluster, options);

        return TopicDataResultNextList.of(
            data,
            options.after(data, uri),
            (options.getPartition() == null ? topic.getSize() : topic.getSize(options.getPartition())),
            topic.canDeleteRecords(cluster, configRepository)
        );
    }

    @AKHQSecured(resource = Role.Resource.TOPIC, action = Role.Action.READ)
    @Get("api/{cluster}/topic/{topicName}")
    @Operation(tags = {"topic"}, summary = "Retrieve a topic")
    public Topic home(String cluster, String topicName) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, topicName);

        return this.topicRepository.findByName(cluster, topicName);
    }

    @AKHQSecured(resource = Role.Resource.TOPIC_DATA, action = Role.Action.READ)
    @Get("api/{cluster}/topic/last-record")
    @Operation(tags = {"topic"}, summary = "Retrieve the last record for a list of topics")
    public Map<String, Record> lastRecord(String cluster, List<String> topics) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, topics);

        return this.recordRepository.getLastRecord(cluster, topics);
    }

    @AKHQSecured(resource = Role.Resource.TOPIC, action = Role.Action.READ)
    @Get("api/{cluster}/topic/{topicName}/partitions")
    @Operation(tags = {"topic"}, summary = "List all partition from a topic")
    public List<Partition> partitions(String cluster, String topicName) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, topicName);

        return this.topicRepository.findByName(cluster, topicName).getPartitions();
    }

    @AKHQSecured(resource = Role.Resource.CONSUMER_GROUP, action = Role.Action.READ)
    @Get("api/{cluster}/topic/{topicName}/groups")
    @Operation(tags = {"topic"}, summary = "List all consumer groups from a topic")
    public List<ConsumerGroup> groups(String cluster, String topicName) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, topicName);

        return this.consumerGroupRepository.findByTopic(cluster, topicName,
            buildUserBasedResourceFilters(cluster));
    }

    @AKHQSecured(resource = Role.Resource.TOPIC, action = Role.Action.READ_CONFIG)
    @Get("api/{cluster}/topic/{topicName}/configs")
    @Operation(tags = {"topic"}, summary = "List all configs from a topic")
    public List<Config> config(String cluster, String topicName) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, topicName);

        return this.configRepository.findByTopic(cluster, topicName);
    }

    @AKHQSecured(resource = Role.Resource.TOPIC, action = Role.Action.READ)
    @Get("api/{cluster}/topic/{topicName}/logs")
    @Operation(tags = {"topic"}, summary = "List all logs from a topic")
    public List<LogDir> logs(String cluster, String topicName) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, topicName);

        return this.topicRepository.findByName(cluster, topicName).getLogDir();
    }

    @AKHQSecured(resource = Role.Resource.TOPIC, action = Role.Action.READ)
    @Get("api/{cluster}/topic/{topicName}/acls")
    @Operation(tags = {"topic"}, summary = "List all acls from a topic")
    public List<AccessControl> acls(String cluster, String topicName) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, topicName);

        return aclRepository.findByResourceType(cluster, ResourceType.TOPIC, topicName);
    }

    @AKHQSecured(resource = Role.Resource.TOPIC, action = Role.Action.ALTER_CONFIG)
    @Post(value = "api/{cluster}/topic/{topicName}/configs")
    @Operation(tags = {"topic"}, summary = "Update configs from a topic")
    public List<Config> updateConfig(String cluster, String topicName, @Body("configs") Map<String, String> configs) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, topicName);

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

    @AKHQSecured(resource =  Role.Resource.TOPIC, action =  Role.Action.UPDATE)
    @Post(value = "api/{cluster}/topic/{topicName}/partitions")
    @Operation(tags = {"topic"}, summary = "Increase partition for a topic")
    public HttpResponse<?> increasePartition(String cluster, String topicName, @Body Map<String, Integer> config) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, topicName);
        this.topicRepository.increasePartition(cluster, topicName, config.get("partition"));

        return HttpResponse.accepted();
    }

    @AKHQSecured(resource = Role.Resource.TOPIC_DATA, action = Role.Action.DELETE)
    @Delete("api/{cluster}/topic/{topicName}/data/empty")
    @Operation(tags = {"topic data"}, summary = "Empty data from a topic")
    public HttpResponse<?> emptyTopic(String cluster, String topicName) throws ExecutionException, InterruptedException{
        checkIfClusterAndResourceAllowed(cluster, topicName);

        this.recordRepository.emptyTopic(
                cluster,
                topicName
        );

        return HttpResponse.noContent();
    }

    @AKHQSecured(resource = Role.Resource.TOPIC_DATA, action = Role.Action.DELETE)
    @Delete("api/{cluster}/topic/{topicName}/data")
    @Operation(tags = {"topic data"}, summary = "Delete data from a topic by key")
    public Record deleteRecordApi(String cluster, String topicName, Integer partition, String key) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, topicName);

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
            new ArrayList<>(),
            topicRepository.findByName(cluster, topicName),
            null
        );
    }

    @AKHQSecured(resource = Role.Resource.TOPIC, action = Role.Action.DELETE)
    @Delete("api/{cluster}/topic/{topicName}")
    @Operation(tags = {"topic"}, summary = "Delete a topic")
    public HttpResponse<?> delete(String cluster, String topicName) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, topicName);

        this.kafkaWrapper.deleteTopics(cluster, topicName);

        return HttpResponse.noContent();
    }

    @AKHQSecured(resource = Role.Resource.TOPIC_DATA, action = Role.Action.READ)
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
        Optional<String> endTimestamp,
        Optional<String> searchByKey,
        Optional<String> searchByValue,
        Optional<String> searchByHeaderKey,
        Optional<String> searchByHeaderValue,
        Optional<String> searchByKeySubject,
        Optional<String> searchByValueSubject
    ) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, topicName);

        RecordRepository.Options options = dataSearchOptions(
            cluster,
            topicName,
            after,
            partition,
            sort,
            timestamp,
            endTimestamp,
            searchByKey,
            searchByValue,
            searchByHeaderKey,
            searchByHeaderValue,
            searchByKeySubject,
            searchByValueSubject
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

    @AKHQSecured(resource = Role.Resource.TOPIC_DATA, action = Role.Action.READ)
    @ExecuteOn(TaskExecutors.IO)
    @Get(value = "api/{cluster}/topic/{topicName}/data/download")
    @Operation(tags = {"topic data download"}, summary = "Download data for a topic")
    public HttpResponse<StreamedFile> download(
        String cluster,
        String topicName,
        Optional<String> after,
        Optional<Integer> partition,
        Optional<RecordRepository.Options.Sort> sort,
        Optional<String> timestamp,
        Optional<String> endTimestamp,
        Optional<String> searchByKey,
        Optional<String> searchByValue,
        Optional<String> searchByHeaderKey,
        Optional<String> searchByHeaderValue,
        Optional<String> searchByKeySubject,
        Optional<String> searchByValueSubject
    ) throws ExecutionException, InterruptedException, IOException {
        checkIfClusterAndResourceAllowed(cluster, topicName);

        RecordRepository.Options options = dataSearchOptions(
            cluster,
            topicName,
            after,
            partition,
            sort,
            timestamp,
            endTimestamp,
            searchByKey,
            searchByValue,
            searchByHeaderKey,
            searchByHeaderValue,
            searchByKeySubject,
            searchByValueSubject
        );

        // Set in MAX_POLL_RECORDS_CONFIG, big number increases speed
        options.setSize(10000);

        Topic topic = topicRepository.findByName(cluster, topicName);

        ObjectMapper mapper = JsonMapper.builder()
            .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
            .build();
        // For ZonedDatetime serialization
        mapper.findAndRegisterModules();
        mapper.setConfig(mapper.getSerializationConfig().withView(Record.Views.Download.class));

        PipedOutputStream out = new PipedOutputStream();
        PipedInputStream in = new PipedInputStream(out);

        new Thread(() -> {
            try (out) {
                out.write('[');

                AtomicBoolean continueSearch = new AtomicBoolean(true);
                AtomicBoolean isFirstBatch = new AtomicBoolean(true);

                while(continueSearch.get()) {
                    recordRepository
                        .search(topic, options)
                        .observeOn(Schedulers.io())
                        .map(event -> {
                            if (!event.getData().getRecords().isEmpty()) {
                                if (!isFirstBatch.getAndSet(false)) {
                                    // Add a comma between batches records
                                    out.write(',');
                                }

                                byte[] bytes = mapper.writeValueAsString(event.getData().getRecords()).getBytes();
                                // Remove start [ and end ] to concatenate records in the same array
                                out.write(Arrays.copyOfRange(bytes, 1, bytes.length - 1));

                            } else {
                                // No more records, add the end array ] and stop here
                                if (event.getData().getEmptyPoll() == 1) {
                                    out.write(']');
                                    out.flush();
                                    continueSearch.set(false);
                                }
                                else if (event.getData().getAfter() != null) {
                                    // Continue to search from the last offsets
                                    options.setAfter(event.getData().getAfter());
                                }
                            }

                            return 0;
                        }).blockingSubscribe();
                }
            } catch (IOException | ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        return HttpResponse.ok(new StreamedFile(in, MediaType.APPLICATION_JSON_TYPE));
    }


    @AKHQSecured(resource = Role.Resource.TOPIC_DATA, action = Role.Action.READ)
    @Get("api/{cluster}/topic/{topicName}/data/record/{partition}/{offset}")
    @Operation(tags = {"topic data"}, summary = "Get a single record by partition and offset")
    public ResultNextList<Record> record(
            HttpRequest<?> request,
            String cluster,
            String topicName,
            Integer partition,
            Long offset
    ) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, topicName);

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

    @AKHQSecured(resource = Role.Resource.TOPIC_DATA, action = Role.Action.READ)
    @Get("api/{cluster}/topic/{topicName}/offsets/start")
    @Operation(tags = {"topic data"}, summary = "Get topic partition offsets by timestamp")
    public List<RecordRepository.TimeOffset> offsetsStart(String cluster, String topicName, Optional<Instant> timestamp) throws ExecutionException, InterruptedException {
        checkIfClusterAndResourceAllowed(cluster, topicName);

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


    @AKHQSecured(resource = Role.Resource.TOPIC_DATA, action = Role.Action.CREATE)
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
        checkIfClusterAndResourceAllowed(fromCluster, fromTopicName);
        checkIfClusterAndResourceAllowed(toCluster, toTopicName);

        Topic fromTopic = this.topicRepository.findByName(fromCluster, fromTopicName);
        Topic toTopic = this.topicRepository.findByName(toCluster, toTopicName);

        if (!CollectionUtils.isNotEmpty(offsets)) {
            throw new IllegalArgumentException("Empty collections");
        }

        if (fromCluster.equals(toCluster) && fromTopicName.equals(toTopicName)) {
            // #745 Prevent endless loop when copying topic onto itself; Use intermediate copy topic for duplication
            throw new IllegalArgumentException("Can not copy topic to itself");
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
        Optional<String> endTimestamp,
        Optional<String> searchByKey,
        Optional<String> searchByValue,
        Optional<String> searchByHeaderKey,
        Optional<String> searchByHeaderValue,
        Optional<String> searchByKeySubject,
        Optional<String> searchByValueSubject
    ) {
        RecordRepository.Options options = new RecordRepository.Options(environment, cluster, topicName);

        after.ifPresent(options::setAfter);
        partition.ifPresent(options::setPartition);
        sort.ifPresent(options::setSort);
        timestamp.map(r -> Instant.parse(r).toEpochMilli()).ifPresent(options::setTimestamp);
        endTimestamp.map(r -> Instant.parse(r).toEpochMilli()).ifPresent(options::setEndTimestamp);
        after.ifPresent(options::setAfter);
        searchByKey.ifPresent(options::setSearchByKey);
        searchByValue.ifPresent(options::setSearchByValue);
        searchByHeaderKey.ifPresent(options::setSearchByHeaderKey);
        searchByHeaderValue.ifPresent(options::setSearchByHeaderValue);
        searchByKeySubject.ifPresent(options::setSearchByKeySubject);
        searchByValueSubject.ifPresent(options::setSearchByValueSubject);
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

