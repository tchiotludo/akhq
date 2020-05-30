package org.akhq.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.sse.Event;
import io.micronaut.security.annotation.Secured;
import io.micronaut.views.View;
import io.micronaut.views.freemarker.FreemarkerViewsRenderer;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.akhq.configs.Role;
import org.akhq.models.Record;
import org.akhq.repositories.RecordRepository;
import org.akhq.repositories.TopicRepository;
import org.reactivestreams.Publisher;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Secured(Role.ROLE_TOPIC_READ)
@Controller("${akhq.server.base-path:}/")
public class TailController extends AbstractController {
    private final RecordRepository recordRepository;
    private final TopicRepository topicRepository;
    private final FreemarkerViewsRenderer freemarkerViewsRenderer;

    @Inject
    public TailController(
        RecordRepository recordRepository,
        TopicRepository topicRepository,
        FreemarkerViewsRenderer freemarkerViewsRenderer
    ) {
        this.recordRepository = recordRepository;
        this.topicRepository = topicRepository;
        this.freemarkerViewsRenderer = freemarkerViewsRenderer;
    }

    @View("tail")
    @Get("{cluster}/tail")
    @Hidden
    public HttpResponse list(
        HttpRequest request,
        String cluster,
        Optional<List<String>> topics,
        Optional<String> search,
        Optional<Integer> size
    ) throws ExecutionException, InterruptedException {
        return this.template(
            request,
            cluster,
            "search", search,
            "topics", topics.orElse(new ArrayList<>()),
            "topicsList", this.topicRepository.all(cluster, TopicRepository.TopicListView.ALL, Optional.empty()),
            "size", size.orElse(100)
        );
    }

    @Secured(Role.ROLE_TOPIC_DATA_READ)
    @Get("{cluster}/tail/sse")
    @Hidden
    public Publisher<Event<?>> sse(
        String cluster,
        List<String> topics,
        Optional<String> search,
        Optional<List<String>> after
    ) {
        RecordRepository.TailOptions options = new RecordRepository.TailOptions(cluster, topics);
        search.ifPresent(options::setSearch);
        after.ifPresent(options::setAfter);

        Map<String, Object> datas = new HashMap<>();
        datas.put("clusterId", cluster);
        datas.put("basePath", getBasePath());
        datas.put("canDeleteRecords", false);
        datas.put("displayTopic", true);
        datas.put("roles", getRights());

        return recordRepository
            .tail(cluster, options)
            .map(event -> {
                TailBody tailBody = new TailBody();
                tailBody.offsets = getOffsets(event);

                if (event.getData().getRecords().size() > 0) {
                    datas.put("datas", event.getData().getRecords());

                    StringWriter stringWriter = new StringWriter();
                    try {
                        freemarkerViewsRenderer.render("topicSearch", datas).writeTo(stringWriter);
                    } catch (IOException ignored) {
                    }

                    tailBody.body = stringWriter.toString();
                }

                return Event
                    .of(tailBody)
                    .name(event.getName());
            });
    }

    @Secured(Role.ROLE_TOPIC_DATA_READ)
    @Get("api/{cluster}/tail/sse")
    @Operation(tags = {"topic data"}, summary = "Tail for data on multiple topic")
    public Publisher<Event<TailRecord>> sseApi(
        String cluster,
        List<String> topics,
        Optional<String> search,
        Optional<List<String>> after
    ) {
        RecordRepository.TailOptions options = new RecordRepository.TailOptions(cluster, topics);
        search.ifPresent(options::setSearch);
        after.ifPresent(options::setAfter);

        return recordRepository
            .tail(cluster, options)
            .map(event -> {
                TailRecord tailRecord = new TailRecord();
                tailRecord.offsets = getOffsets(event);

                if (event.getData().getRecords().size() > 0) {
                    tailRecord.records = event.getData().getRecords();
                }

                return Event
                    .of(tailRecord)
                    .name(event.getName());
            });
    }

    private static List<String> getOffsets(Event<RecordRepository.TailEvent> event) {
        return event.getData()
            .getOffsets()
            .entrySet()
            .stream()
            .flatMap(s -> s.getKey()
                .entrySet()
                .stream()
                .map(r -> {
                    List<String> strings = Arrays.asList(
                        r.getKey(),
                        String.valueOf(r.getValue()),
                        String.valueOf(s.getValue() + 1)
                    );

                    return String.join(
                        ",",
                        strings
                    );
                })
            )
            .collect(Collectors.toList());
    }

    @ToString
    @EqualsAndHashCode
    public static class TailBody {
        @JsonProperty("body")
        private String body;

        @JsonProperty("offsets")
        private List<String> offsets = new ArrayList<>();

    }

    @ToString
    @EqualsAndHashCode
    @Getter
    public static class TailRecord {
        @JsonProperty("records")
        private List<Record> records;

        @JsonProperty("offsets")
        private List<String> offsets = new ArrayList<>();

    }
}
