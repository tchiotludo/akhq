package org.akhq.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.sse.Event;
import io.micronaut.security.annotation.Secured;
import io.micronaut.views.View;
import io.micronaut.views.freemarker.FreemarkerViewsRenderer;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.akhq.models.*;
import org.akhq.utils.ResultNextList;
import org.akhq.utils.ResultPagedList;
import org.apache.kafka.common.resource.ResourceType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.akhq.configs.Role;
import org.akhq.middlewares.SchemaComparator;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.modules.RequestHelper;
import org.akhq.repositories.AccessControlListRepository;
import org.akhq.repositories.ConfigRepository;
import org.akhq.repositories.RecordRepository;
import org.akhq.repositories.SchemaRegistryRepository;
import org.akhq.repositories.TopicRepository;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;
import org.reactivestreams.Publisher;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
    private final FreemarkerViewsRenderer freemarkerViewsRenderer;
    private final Environment environment;
    private final AccessControlListRepository aclRepository;
    private final SchemaRegistryRepository schemaRegistryRepository;

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
        FreemarkerViewsRenderer freemarkerViewsRenderer,
        Environment environment,
        AccessControlListRepository aclRepository,
        SchemaRegistryRepository schemaRegistryRepository
    ) {
        this.kafkaWrapper = kafkaWrapper;
        this.topicRepository = topicRepository;
        this.configRepository = configRepository;
        this.recordRepository = recordRepository;
        this.freemarkerViewsRenderer = freemarkerViewsRenderer;
        this.environment = environment;
        this.aclRepository = aclRepository;
        this.schemaRegistryRepository = schemaRegistryRepository;
    }

    @View("topicList")
    @Get("{cluster}/topic")
    @Hidden
    public HttpResponse<?> list(
        HttpRequest<?> request,
        String cluster,
        Optional<String> search,
        Optional<TopicRepository.TopicListView> show,
        Optional<Integer> page
    ) throws ExecutionException, InterruptedException {
        TopicRepository.TopicListView topicListView = show.orElse(TopicRepository.TopicListView.valueOf(defaultView));

        URIBuilder uri = URIBuilder.fromURI(request.getUri());
        Pagination pagination = new Pagination(pageSize, uri, page.orElse(1));

        PagedList<Topic> list = this.topicRepository.list(
            cluster,
            pagination,
            show.orElse(TopicRepository.TopicListView.valueOf(defaultView)),
            search
        );

        return this.template(
            request,
            cluster,
            "search", search,
            "topicListView", topicListView,
            "topics", list,
            "skipConsumerGroups", skipConsumerGroups,
            "pagination", ImmutableMap.builder()
                .put("size", list.total())
                .put("before", list.before().toNormalizedURI(false).toString())
                .put("after", list.after().toNormalizedURI(false).toString())
                .build()
        );
    }

    @Get("api/{cluster}/topic")
    @Operation(tags = {"topic"}, summary = "List all topics")
    public ResultPagedList<Topic> listApi(
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
    @View("topicCreate")
    @Get("{cluster}/topic/create")
    @Hidden
    public HttpResponse<?> create(HttpRequest<?> request, String cluster) {
        return this.template(
            request,
            cluster,
            "replication", this.replicationFactor,
            "retention", this.retentionPeriod.toString(),
            "partition", this.partitionCount
        );
    }

    @Secured(Role.ROLE_TOPIC_INSERT)
    @Post(value = "{cluster}/topic/create", consumes = MediaType.MULTIPART_FORM_DATA)
    @Hidden
    public HttpResponse<?> createSubmit(
        HttpRequest<?> request,
        String cluster,
        String name,
        Integer partition,
        Short replication,
        Map<String, String> configs
    ) throws Throwable {
        List<Config> options = configs
            .entrySet()
            .stream()
            .filter(r -> r.getKey().startsWith("configs"))
            .map(r -> new AbstractMap.SimpleEntry<>(
                r.getKey().replaceAll("(configs\\[)(.*)(])", "$2"),
                r.getValue()
            ))
            .map(r -> new Config(r.getKey(), r.getValue()))
            .collect(Collectors.toList());

        MutableHttpResponse<Void> response = HttpResponse.redirect(this.uri("/" + cluster + "/topic"));

        this.toast(response, RequestHelper.runnableToToast(
            () ->
                this.topicRepository.create(
                    cluster,
                    name,
                    partition,
                    replication,
                    options
                ),
            "Topic '" + name + "' is created",
            "Failed to create topic '" + name + "'"
        ));

        return response;
    }

    @Secured(Role.ROLE_TOPIC_INSERT)
    @Post(value = "api/{cluster}/topic")
    @Operation(tags = {"topic"}, summary = "Create a topic")
    public Topic createApi(
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
    @View("topicProduce")
    @Get("{cluster}/topic/{topicName}/produce")
    @Hidden
    public HttpResponse<?> produce(HttpRequest<?> request, String cluster, String topicName) throws ExecutionException, InterruptedException, IOException, RestClientException {
        Topic topic = this.topicRepository.findByName(cluster, topicName);

        List<Schema> schemas = this.schemaRegistryRepository.listAll(cluster, Optional.empty());
        List<Schema> keySchemas = schemas.stream()
                .filter(schema -> !schema.getSubject().endsWith(VALUE_SUFFIX))
                .sorted(new SchemaComparator(topicName, true))
                .collect(Collectors.toList());
        List<Schema> valueSchemas = schemas.stream()
                .filter(schema -> !schema.getSubject().endsWith(KEY_SUFFIX))
                .sorted(new SchemaComparator(topicName, false))
                .collect(Collectors.toList());

        return this.template(
            request,
            cluster,
            "topic", topic,
                "keySchemasList", keySchemas,
                "valueSchemasList", valueSchemas
        );
    }

    @Secured(Role.ROLE_TOPIC_DATA_INSERT)
    @Post(value = "{cluster}/topic/{topicName}/produce", consumes = MediaType.MULTIPART_FORM_DATA)
    @Hidden
    public HttpResponse<?> produceSubmit(
        HttpRequest<?> request,
        String cluster,
        String topicName,
        String value,
        Optional<String> key,
        Optional<Integer> partition,
        Optional<String> timestamp,
        Map<String, List<String>> headers,
        Optional<Integer> keySchema,
        Optional<Integer> valueSchema
    ) {
        Map<String, String> finalHeaders = new HashMap<>();

        int i = 0;
        for (String headerKey : headers.get("headers[key]")) {
            if (headerKey != null && !headerKey.equals("") && headers.get("headers[value]").get(i) != null) {
                finalHeaders.put(
                    headerKey,
                    headers.get("headers[value]").get(i).equals("") ? null : headers.get("headers[value]").get(i)
                );
            }
            i++;
        }

        MutableHttpResponse<Void> response = HttpResponse.redirect(request.getUri());

        this.toast(response, RequestHelper.runnableToToast(() ->
                this.recordRepository.produce(
                    cluster,
                    topicName,
                    value,
                    finalHeaders,
                    key.filter(r -> !r.equals("")),
                    partition,
                    timestamp.filter(r -> !r.equals("")).map(r -> Instant.parse(r).toEpochMilli()),
                    keySchema,
                    valueSchema
                )
            ,
            "Record created",
            "Failed to produce record"
        ));

        return response;
    }

    @Secured(Role.ROLE_TOPIC_DATA_INSERT)
    @Post(value = "api/{cluster}/topic/{topicName}/data")
    @Operation(tags = {"topic data"}, summary = "Produce data to a topic")
    public Record produceApi(
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
    @View("topic")
    @Get("{cluster}/topic/{topicName}")
    @Hidden
    public HttpResponse<?> data(
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

        List<Record> data = new ArrayList<>();

        if (options.getSearch() == null) {
            data = this.recordRepository.consume(cluster, options);
        }

        URIBuilder uri = URIBuilder.fromURI(request.getUri());

        ImmutableMap.Builder<String, String> partitionUrls = ImmutableSortedMap.naturalOrder();
        partitionUrls.put((uri.getParametersByName("partition").size() > 0 ? uri.removeParameters("partition") : uri).toNormalizedURI(false).toString(), "All");
        for (int i = 0; i < topic.getPartitions().size(); i++) {
            partitionUrls.put(uri.addParameter("partition", String.valueOf(i)).toNormalizedURI(false).toString(), String.valueOf(i));
        }

        return this.template(
            request,
            cluster,
            "tab", "data",
            "topic", topic,
            "canDeleteRecords", topic.canDeleteRecords(cluster, configRepository),
            "datas", data,
            "partitions", topic.getPartitions().size(),
            "navbar", dataNavbar(options, uri, partitionUrls),
            "pagination", dataPagination(topic, options, data, uri)
        );
    }

    @Secured(Role.ROLE_TOPIC_DATA_READ)
    @Get("api/{cluster}/topic/{topicName}/data")
    @Operation(tags = {"topic data"}, summary = "Read datas from a topic")
    public ResultNextList<Record> dataApi(
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

        return ResultNextList.of(
            data,
            options.after(data, uri),
            (options.getPartition() == null ? topic.getSize() : topic.getSize(options.getPartition()))
        );
    }

    private ImmutableMap<Object, Object> dataPagination(Topic topic, RecordRepository.Options options, List<Record> data, URIBuilder uri) {
        return ImmutableMap.builder()
            .put("size", "≈ " + (options.getPartition() == null ? topic.getSize() : topic.getSize(options.getPartition())))
            // .put("before", options.before(data, uri).toNormalizedURI(false).toString())
            .put("after", options.after(data, uri).toNormalizedURI(false).toString())
            .build();
    }

    private ImmutableMap<Object, Object> dataNavbar(RecordRepository.Options options, URIBuilder uri, ImmutableMap.Builder<String, String> partitionUrls) {
        return ImmutableMap.builder()
            .put("partition", ImmutableMap.builder()
                .put("current", Optional.ofNullable(options.getPartition()))
                .put("values", partitionUrls.build())
                .build()
            )
            .put("sort", ImmutableMap.builder()
                .put("current", Optional.ofNullable(options.getSort()))
                .put("values", ImmutableMap.builder()
                    .put(uri.addParameter("sort", RecordRepository.Options.Sort.NEWEST.name()).toNormalizedURI(false).toString(), RecordRepository.Options.Sort.NEWEST.name())
                    .put(uri.addParameter("sort", RecordRepository.Options.Sort.OLDEST.name()).toNormalizedURI(false).toString(), RecordRepository.Options.Sort.OLDEST.name())
                    .build()
                )
                .build()
            )
            .put("timestamp", ImmutableMap.builder()
                .put("current", Optional.ofNullable(options.getTimestamp()))
                .build()
            )
            .put("search", ImmutableMap.builder()
                .put("current", Optional.ofNullable(options.getSearch()))
                .build()
            )
            .put("offset", ImmutableMap.builder()
                .putAll(options.getAfter().entrySet().stream().collect(Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        Map.Entry::getValue
                )))
                .build()
            ).build();
    }

    @Secured(Role.ROLE_TOPIC_READ)
    @View("topic")
    @Get("{cluster}/topic/{topicName}/{tab:(partitions|groups|configs|logs|acls)}")
    @Hidden
    public HttpResponse<?> tab(HttpRequest<?> request, String cluster, String topicName, String tab) throws ExecutionException, InterruptedException {
        return this.render(request, cluster, topicName,  tab);
    }

    private HttpResponse<?> render(HttpRequest<?> request, String cluster, String topicName, String tab) throws ExecutionException, InterruptedException {
        Topic topic = this.topicRepository.findByName(cluster, topicName);
        List<Config> configs = this.configRepository.findByTopic(cluster, topicName);

        return this.template(
            request,
            cluster,
            "tab", tab,
            "topic", topic,
            "acls", aclRepository.findByResourceType(cluster, ResourceType.TOPIC, topic.getName()),
            "configs", configs
        );
    }

    @Get("api/{cluster}/topic/{topicName}")
    @Operation(tags = {"topic"}, summary = "Retrieve a topic")
    public Topic homeApi(String cluster, String topicName) throws ExecutionException, InterruptedException {
        return this.topicRepository.findByName(cluster, topicName);
    }

    @Get("api/{cluster}/topic/{topicName}/partitions")
    @Operation(tags = {"topic"}, summary = "List all partition from a topic")
    public List<Partition> partitionsApi(String cluster, String topicName) throws ExecutionException, InterruptedException {
        return this.topicRepository.findByName(cluster, topicName).getPartitions();
    }

    @Get("api/{cluster}/topic/{topicName}/groups")
    @Operation(tags = {"topic"}, summary = "List all consumer groups from a topic")
    public List<ConsumerGroup> groupsApi(String cluster, String topicName) throws ExecutionException, InterruptedException {
        return this.topicRepository.findByName(cluster, topicName).getConsumerGroups();
    }

    @Get("api/{cluster}/topic/{topicName}/configs")
    @Operation(tags = {"topic"}, summary = "List all configs from a topic")
    public List<Config> configApi(String cluster, String topicName) throws ExecutionException, InterruptedException {
        return this.configRepository.findByTopic(cluster, topicName);
    }

    @Get("api/{cluster}/topic/{topicName}/logs")
    @Operation(tags = {"topic"}, summary = "List all logs from a topic")
    public List<LogDir> logsApi(String cluster, String topicName) throws ExecutionException, InterruptedException {
        return this.topicRepository.findByName(cluster, topicName).getLogDir();
    }

    @Get("api/{cluster}/topic/{topicName}/acls")
    @Operation(tags = {"topic"}, summary = "List all acls from a topic")
    public List<AccessControl> aclsApi(String cluster, String topicName) throws ExecutionException, InterruptedException {
        return aclRepository.findByResourceType(cluster, ResourceType.TOPIC, topicName);
    }

    @Secured(Role.ROLE_TOPIC_CONFIG_UPDATE)
    @Post(value = "{cluster}/topic/{topicName}/configs", consumes = MediaType.MULTIPART_FORM_DATA)
    @Hidden
    public HttpResponse<?> updateConfig(HttpRequest<?> request, String cluster, String topicName, Map<String, String> configs) throws Throwable {
        List<Config> updated = ConfigRepository.updatedConfigs(configs, this.configRepository.findByTopic(cluster, topicName), true);
        MutableHttpResponse<Void> response = HttpResponse.redirect(request.getUri());

        this.toast(response, RequestHelper.runnableToToast(() -> {
                if (updated.size() == 0) {
                    throw new IllegalArgumentException("No config to update");
                }

                this.configRepository.updateTopic(
                    cluster,
                    topicName,
                    updated
                );
            },
            "Topic configs '" + topicName + "' is updated",
            "Failed to update topic '" + topicName + "' configs"
        ));

        return response;
    }

    @Secured(Role.ROLE_TOPIC_CONFIG_UPDATE)
    @Post(value = "api/{cluster}/topic/{topicName}/configs")
    @Operation(tags = {"topic"}, summary = "Update configs from a topic")
    public List<Config> updateConfigApi(String cluster, String topicName, Map<String, String> configs) throws ExecutionException, InterruptedException {
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
    @Get("{cluster}/topic/{topicName}/deleteRecord")
    @Hidden
    public HttpResponse<?> deleteRecord(String cluster, String topicName, Integer partition, String key) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() -> this.recordRepository.delete(
                cluster,
                topicName,
                partition,
                Base64.getDecoder().decode(key)
            ),
            "Record '" + key + "' will be deleted on compaction",
            "Failed to delete record '" + key + "'"
        ));

        return response;
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
            Base64.getDecoder().decode(key),
            null,
            new HashMap<>()
        );
    }

    @Secured(Role.ROLE_TOPIC_DELETE)
    @Get("{cluster}/topic/{topicName}/delete")
    @Hidden
    public HttpResponse<?> delete(String cluster, String topicName) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
                this.kafkaWrapper.deleteTopics(cluster, topicName),
            "Topic '" + topicName + "' is deleted",
            "Failed to delete topic " + topicName
        ));

        return response;
    }

    @Secured(Role.ROLE_TOPIC_DELETE)
    @Delete("api/{cluster}/topic/{topicName}")
    @Operation(tags = {"topic"}, summary = "Delete a topic")
    public HttpResponse<?> deleteApi(String cluster, String topicName) throws ExecutionException, InterruptedException {
        this.kafkaWrapper.deleteTopics(cluster, topicName);

        return HttpResponse.noContent();
    }

    @Secured(Role.ROLE_TOPIC_DATA_READ)
    @Get("{cluster}/topic/{topicName}/search/{search}")
    @Hidden
    public Publisher<Event<?>> sse(
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

        Map<String, Object> datas = new HashMap<>();
        datas.put("topic", topic);
        datas.put("canDeleteRecords", topic.canDeleteRecords(cluster, configRepository));
        datas.put("clusterId", cluster);
        datas.put("basePath", getBasePath());
        datas.put("roles", getRights());

        return recordRepository
            .search(cluster, options)
            .map(event -> {
                SearchBody searchBody = new SearchBody(
                    event.getData().getPercent(),
                    event.getData().getAfter()
                );

                if (event.getData().getRecords().size() > 0) {
                    datas.put("datas", event.getData().getRecords());
                    StringWriter stringWriter = new StringWriter();
                    try {
                        freemarkerViewsRenderer.render("topicSearch", datas).writeTo(stringWriter);
                    } catch (IOException ignored) {
                    }

                    searchBody.body = stringWriter.toString();
                }

                return Event
                    .of(searchBody)
                    .name(event.getName());
            });
    }

    @Secured(Role.ROLE_TOPIC_DATA_READ)
    @Get("api/{cluster}/topic/{topicName}/data/search/{search}")
    @Operation(tags = {"topic data"}, summary = "Search for data for a topic")
    public Publisher<Event<SearchRecord>> sseApi(
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
    public static class SearchBody {
        public SearchBody(double percent, String after) {
            this.percent = percent;
            this.after = after;
        }

        @JsonProperty("percent")
        private final Double percent;

        @JsonProperty("body")
        private String body;

        @JsonProperty("after")
        private final String after;
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
