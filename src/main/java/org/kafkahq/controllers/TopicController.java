package org.kafkahq.controllers;

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
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.sse.Event;
import io.micronaut.security.annotation.Secured;
import io.micronaut.views.View;
import io.micronaut.views.freemarker.FreemarkerViewsRenderer;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.resource.ResourceType;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.kafkahq.configs.Role;
import org.kafkahq.middlewares.SchemaComparator;
import org.kafkahq.models.Config;
import org.kafkahq.models.Record;
import org.kafkahq.models.Schema;
import org.kafkahq.models.Topic;
import org.kafkahq.modules.AbstractKafkaWrapper;
import org.kafkahq.modules.RequestHelper;
import org.kafkahq.repositories.AccessControlListRepository;
import org.kafkahq.repositories.ConfigRepository;
import org.kafkahq.repositories.RecordRepository;
import org.kafkahq.repositories.SchemaRegistryRepository;
import org.kafkahq.repositories.TopicRepository;
import org.kafkahq.utils.PagedList;
import org.kafkahq.utils.Pagination;
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
@Controller("${kafkahq.server.base-path:}/{cluster}/topic")
public class TopicController extends AbstractController {
    public static final String VALUE_SUFFIX = "-value";
    public static final String KEY_SUFFIX = "-key";
    private AbstractKafkaWrapper kafkaWrapper;
    private TopicRepository topicRepository;
    private ConfigRepository configRepository;
    private RecordRepository recordRepository;
    private FreemarkerViewsRenderer freemarkerViewsRenderer;
    private Environment environment;
    private AccessControlListRepository aclRepository;
    private SchemaRegistryRepository schemaRegistryRepository;

    @Value("${kafkahq.topic.default-view}")
    private String defaultView;
    @Value("${kafkahq.topic.replication}")
    private Integer replicationFactor;
    @Value("${kafkahq.topic.retention}")
    private Integer retentionPeriod;
    @Value("${kafkahq.topic.partition}")
    private Integer partitionCount;
    @Value("${kafkahq.topic.skip-consumer-groups}")
    protected Boolean skipConsumerGroups;
    @Value("${kafkahq.pagination.page-size}")
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
    @Get
    public HttpResponse list(
        HttpRequest request, String cluster,
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

    @Secured(Role.ROLE_TOPIC_INSERT)
    @View("topicCreate")
    @Get("create")
    public HttpResponse create(HttpRequest request, String cluster) {
        return this.template(
            request,
            cluster,
            "replication", this.replicationFactor,
            "retention", this.retentionPeriod.toString(),
            "partition", this.partitionCount
        );
    }

    @Secured(Role.ROLE_TOPIC_INSERT)
    @Post(value = "create", consumes = MediaType.MULTIPART_FORM_DATA)
    public HttpResponse createSubmit(HttpRequest request,
                                     String cluster,
                                     String name,
                                     Integer partition,
                                     Short replication,
                                     Map<String, String> configs)
        throws Throwable
    {
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

        this.toast(response, RequestHelper.runnableToToast(() ->
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

    @Secured(Role.ROLE_TOPIC_DATA_INSERT)
    @View("topicProduce")
    @Get("{topicName}/produce")
    public HttpResponse produce(HttpRequest request, String cluster, String topicName) throws ExecutionException, InterruptedException, IOException, RestClientException {
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
    @Post(value = "{topicName}/produce", consumes = MediaType.MULTIPART_FORM_DATA)
    public HttpResponse produceSubmit(HttpRequest request,
                                      String cluster,
                                      String topicName,
                                      String value,
                                      Optional<String> key,
                                      Optional<Integer> partition,
                                      Optional<String> timestamp,
                                      Map<String, List<String>> headers,
                                      Optional<Integer> keySchema,
                                      Optional<Integer> valueSchema)
    {
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

    @Secured(Role.ROLE_TOPIC_DATA_READ)
    @View("topic")
    @Get("{topicName}")
    public HttpResponse home(HttpRequest request,
                             String cluster,
                             String topicName,
                             Optional<String> after,
                             Optional<Integer> partition,
                             Optional<RecordRepository.Options.Sort> sort,
                             Optional<String> timestamp,
                             Optional<String> search)
        throws ExecutionException, InterruptedException
    {
        Topic topic = this.topicRepository.findByName(cluster, topicName);

        RecordRepository.Options options = new RecordRepository.Options(environment, cluster, topicName);
        after.ifPresent(options::setAfter);
        partition.ifPresent(options::setPartition);
        sort.ifPresent(options::setSort);
        timestamp.map(r -> Instant.parse(r).toEpochMilli()).ifPresent(options::setTimestamp);
        after.ifPresent(options::setAfter);
        search.ifPresent(options::setSearch);

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
    @Get("{topicName}/{tab:(partitions|groups|configs|logs|acls)}")
    public HttpResponse tab(HttpRequest request, String cluster, String topicName, String tab) throws ExecutionException, InterruptedException {
        return this.render(request, cluster, topicName,  tab);
    }

    private HttpResponse render(HttpRequest request, String cluster, String topicName, String tab) throws ExecutionException, InterruptedException {
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

    @Secured(Role.ROLE_TOPIC_CONFIG_UPDATE)
    @Post(value = "{topicName}/configs", consumes = MediaType.MULTIPART_FORM_DATA)
    public HttpResponse updateConfig(HttpRequest request, String cluster, String topicName, Map<String, String> configs) throws Throwable {
        List<Config> updated = ConfigRepository.updatedConfigs(configs, this.configRepository.findByTopic(cluster, topicName));
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

    @Secured(Role.ROLE_TOPIC_DATA_DELETE)
    @Get("{topicName}/deleteRecord")
    public HttpResponse deleteRecord(String cluster, String topicName, Integer partition, String key) {
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

    @Secured(Role.ROLE_TOPIC_DELETE)
    @Get("{topicName}/delete")
    public HttpResponse delete(String cluster, String topicName) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
                this.kafkaWrapper.deleteTopics(cluster, topicName),
            "Topic '" + topicName + "' is deleted",
            "Failed to delete topic " + topicName
        ));

        return response;
    }

    @Secured(Role.ROLE_TOPIC_DATA_READ)
    @Get("{topicName}/search/{search}")
    public Publisher<Event<?>> sse(String cluster,
                                          String topicName,
                                          Optional<String> after,
                                          Optional<Integer> partition,
                                          Optional<RecordRepository.Options.Sort> sort,
                                          Optional<String> timestamp,
                                          Optional<String> search)
        throws ExecutionException, InterruptedException
    {
        Topic topic = topicRepository.findByName(cluster, topicName);

        RecordRepository.Options options = new RecordRepository.Options(environment, cluster, topicName);
        after.ifPresent(options::setAfter);
        partition.ifPresent(options::setPartition);
        sort.ifPresent(options::setSort);
        timestamp.map(r -> Instant.parse(r).toEpochMilli()).ifPresent(options::setTimestamp);
        after.ifPresent(options::setAfter);
        search.ifPresent(options::setSearch);

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
                    } catch (IOException ignored) {}

                    searchBody.body = stringWriter.toString();
                }

                return Event
                    .of(searchBody)
                    .name(event.getName());
            });
    }

    @ToString
    @EqualsAndHashCode
    public static class SearchBody {
        public SearchBody(double percent, String after) {
            this.percent = percent;
            this.after = after;
        }

        @JsonProperty("percent")
        private Double percent;

        @JsonProperty("body")
        private String body;

        @JsonProperty("after")
        private String after;
    }
}
