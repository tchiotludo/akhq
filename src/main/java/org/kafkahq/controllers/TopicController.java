package org.kafkahq.controllers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.inject.Inject;
import org.apache.kafka.common.config.TopicConfig;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.jooby.*;
import org.jooby.mvc.GET;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.kafkahq.models.Config;
import org.kafkahq.models.Record;
import org.kafkahq.models.Topic;
import org.kafkahq.modules.RequestHelper;
import org.kafkahq.repositories.ConfigRepository;
import org.kafkahq.repositories.RecordRepository;
import org.kafkahq.repositories.TopicRepository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Path("/{cluster}/topic")
public class TopicController extends AbstractController {

    @Inject
    private TopicRepository topicRepository;

    @Inject
    private ConfigRepository configRepository;

    @Inject
    private RecordRepository recordRepository;

    @GET
    public View list(Request request, String cluster, Optional<String> search) throws ExecutionException, InterruptedException {
        return this.template(
            request,
            cluster,
            Results
                .html("topicList")
                .put("search", search)
                .put("topics", this.topicRepository.list(search))
        );
    }

    @GET
    @Path("create")
    public View create(Request request, String cluster) {
        return this.template(
            request,
            cluster,
            Results
                .html("topicCreate")
        );
    }

    @POST
    @Path("create")
    public void createSubmit(Request request, Response response, String cluster) throws Throwable {
        List<Config> options = new ArrayList<>();
        Arrays
            .asList(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.RETENTION_MS_CONFIG)
            .forEach(s -> request
                .param("configs[" +  s + "]")
                .toOptional()
                .ifPresent(r -> options.add(new Config(s, r)))
            );

        this.toast(request, RequestHelper.runnableToToast(() ->
                this.topicRepository.create(
                    cluster,
                    request.param("name").value(),
                    request.param("partition").intValue(),
                    request.param("replication").shortValue(),
                    options
                ),
            "Topic '" + request.param("name").value() + "' is created",
            "Failed to create topic '" + request.param("name").value() + "'"
        ));

        response.redirect("/" + cluster + "/topic");
    }

    @GET
    @Path("{topicName}/produce")
    public View produce(Request request, String cluster, String topicName) throws ExecutionException, InterruptedException {
        Topic topic = this.topicRepository.findByName(topicName);

        return this.template(
            request,
            cluster,
            Results
                .html("topicProduce")
                .put("topic", topic)
        );
    }

    @POST
    @Path("{topicName}/produce")
    public void produceSubmit(Request request, Response response, String cluster, String topicName) throws Throwable {
        List<String> headersKey = request.param("headers[key][]").toList();
        List<String> headersValue = request.param("headers[value][]").toList();

        Map<String, String> headers = new HashMap<>();

        int i = 0;
        for (String key : headersKey) {
            if (key != null && !key.equals("") && headersValue.get(i) != null && !headersValue.get(i).equals("")) {
                headers.put(key, headersValue.get(i));
            }
            i++;
        }

        this.toast(request, RequestHelper.runnableToToast(() -> {
                this.recordRepository.produce(
                    cluster,
                    topicName,
                    request.param("value").value(),
                    headers,
                    request.param("key").toOptional(),
                    request.param("partition").toOptional().filter(r -> !r.equals("")).map(Integer::valueOf),
                    request.param("timestamp")
                        .toOptional(String.class)
                        .filter(r -> !r.equals(""))
                        .map(s -> Instant.parse(s).toEpochMilli())
                );
            },
            "Record created",
            "Failed to produce record"
        ));

        response.redirect(request.path());
    }

    @GET
    @Path("{topicName}")
    public View home(Request request, String cluster, String topicName) throws ExecutionException, InterruptedException {
        Topic topic = this.topicRepository.findByName(topicName);
        RecordRepository.Options options = RequestHelper.buildRecordRepositoryOptions(request, cluster, topicName);

        List<Record<String, String>> data = new ArrayList<>();

        if (options.getSearch() == null) {
            data = this.recordRepository.consume(options);
        }

        URIBuilder uri = this.uri(request);

        ImmutableMap.Builder<String, String> partitionUrls = ImmutableSortedMap.naturalOrder();
        partitionUrls.put((uri.getParametersByName("partition").size() > 0 ? uri.removeParameters("partition") : uri).toNormalizedURI(false).toString(), "All");
        for (int i = 0; i < topic.getPartitions().size(); i++) {
            partitionUrls.put(uri.addParameter("partition", String.valueOf(i)).toNormalizedURI(false).toString(), String.valueOf(i));
        }

        return this.template(
            request,
            cluster,
            Results
                .html("topic")
                .put("tab", "data")
                .put("topic", topic)
                .put("canDeleteRecords", topic.canDeleteRecords(configRepository))
                .put("datas", data)
                .put("navbar", dataNavbar(options, uri, partitionUrls))
                .put("pagination", dataPagination(topic, options, data, uri))
        );
    }

    private ImmutableMap<Object, Object> dataPagination(Topic topic, RecordRepository.Options options, List<Record<String, String>> data, URIBuilder uri) {
        return ImmutableMap.builder()
            .put("size", options.getPartition() == null ? topic.getSize() : topic.getSize(options.getPartition()))
            .put("before", options.before(data, uri).toNormalizedURI(false).toString())
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
            .build();
    }

    @GET
    @Path("{topicName}/{tab:(partitions|groups|configs|logs)}")
    public View tab(Request request, String cluster, String topicName, String tab) throws ExecutionException, InterruptedException {
        return this.render(request, cluster, topicName,  tab);
    }

    public View render(Request request, String cluster, String topicName, String tab) throws ExecutionException, InterruptedException {
        Topic topic = this.topicRepository.findByName(topicName);
        List<Config> configs = this.configRepository.findByTopic(topicName);

        return this.template(
            request,
            cluster,
            Results
                .html("topic")
                .put("tab", tab)
                .put("topic", topic)
                .put("configs", configs)
        );
    }

    @POST
    @Path("{topicName}/{tab:configs}")
    public void updateConfig(Request request, Response response, String cluster, String topicName) throws Throwable {
        List<Config> updated = RequestHelper.updatedConfigs(request, this.configRepository.findByTopic(topicName));

        this.toast(request, RequestHelper.runnableToToast(() -> {
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

        response.redirect(request.path());
    }

    @GET
    @Path("{topicName}/deleteRecord")
    public Result deleteRecord(Request request, Response response, String cluster, String topicName, Integer partition, String key) throws Throwable {
        this.toast(request, RequestHelper.runnableToToast(() -> this.recordRepository.delete(
                cluster,
                topicName,
                partition,
                key
            ),
            "Record '" + key + "' will be deleted on compaction",
            "Failed to delete record '" + key + "'"
        ));

        return Results.ok();
    }

    @GET
    @Path("{topicName}/delete")
    public Result delete(Request request, String cluster, String topicName) {
        this.toast(request, RequestHelper.runnableToToast(() ->
                this.topicRepository.delete(cluster, topicName),
            "Topic '" + topicName + "' is deleted",
            "Failed to delete topic " + topicName
        ));

        return Results.ok();
    }
}
