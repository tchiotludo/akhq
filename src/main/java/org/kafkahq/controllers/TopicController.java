package org.kafkahq.controllers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.inject.Inject;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Path("/{cluster}/topic")
public class TopicController extends AbstractController {
    private static final Logger logger = LoggerFactory.getLogger(TopicController.class);

    @Inject
    private TopicRepository topicRepository;

    @Inject
    private ConfigRepository configRepository;

    @Inject
    private RecordRepository recordRepository;

    @GET
    public View list(Request request) throws ExecutionException, InterruptedException {
        return this.template(
            request,
            Results
                .html("topicList")
                .put("topics", this.topicRepository.list())
        );
    }

    @GET
    @Path("{topic}")
    public View home(Request request) throws ExecutionException, InterruptedException {
        Topic topic = this.topicRepository.findByName(request.param("topic").value());
        RecordRepository.Options options = RequestHelper.buildRecordRepositoryOptions(request);

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
            Results
                .html("topic")
                .put("tab", "data")
                .put("topic", topic)
                .put("canDeleteRecords", topic.canDeleteRecords(configRepository))
                .put("datas", data)
                .put("navbar", ImmutableMap.builder()
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
                    .build()
                )
                .put("pagination", ImmutableMap.builder()
                    .put("size", options.getPartition() == null ? topic.getSize() : topic.getSize(options.getPartition()))
                    .put("before", options.before(data, uri).toNormalizedURI(false).toString())
                    .put("after", options.after(data, uri).toNormalizedURI(false).toString())
                    .build()
                )
        );
    }

    @GET
    @Path("{topic}/{tab:(partitions|groups|configs|logs)}")
    public View tab(Request request) throws ExecutionException, InterruptedException {
        return this.topic(request, request.param("tab").value());
    }

    public View topic(Request request, String tab) throws ExecutionException, InterruptedException {
        Topic topic = this.topicRepository.findByName(request.param("topic").value());
        List<Config> configs = this.configRepository.findByTopic(request.param("topic").value());

        return this.template(
            request,
            Results
                .html("topic")
                .put("tab", tab)
                .put("topic", topic)
                .put("configs", configs)
        );
    }

    @POST
    @Path("{topic}/{tab:configs}")
    public void updateConfig(Request request, Response response, String topic) throws Throwable {
        List<Config> configs = this.configRepository.findByTopic(topic);

        List<Config> updated = configs
            .stream()
            .filter(config -> !config.isReadOnly())
            .filter(config -> !config.getValue().equals(request.param("configs[" + config.getName() + "]").value()))
            .map(config -> config.withValue(request.param("configs[" + config.getName() + "]").value()))
            .collect(Collectors.toList());

        this.toast(request, RequestHelper.runnableToToast(() -> {
                if (updated.size() == 0) {
                    throw new IllegalArgumentException("No config to update");
                }

                this.configRepository.updateTopic(
                    request.param("cluster").value(),
                    request.param("topic").value(),
                    updated
                );
            },
            "Topic configs '" + topic + "' is updated",
            "Failed to update topic '" + topic + "' configs"
        ));

        response.redirect(request.path());
    }

    @GET
    @Path("{topic}/deleteRecord")
    public Result deleteRecord(Request request, Response response) throws Throwable {
        String topic = request.param("topic").value();
        Integer partition = request.param("partition").intValue();
        String key = request.param("key").value();

        this.toast(request, RequestHelper.runnableToToast(() -> this.recordRepository.delete(
                request.param("cluster").value(),
                topic,
                partition,
                key
            ),
            "Record '" + key + "' will be deleted on compaction",
            "Failed to delete record '" + key + "'"
        ));

        return Results.ok();
    }

    @GET
    @Path("{topic}/delete")
    public Result delete(Request request, String topic) {
        this.toast(request, RequestHelper.runnableToToast(() ->
                this.topicRepository.delete(request.param("cluster").value(), topic),
            "Topic '" + topic + "' is deleted",
            "Failed to delete topic " + topic
        ));

        return Results.ok();
    }
}
