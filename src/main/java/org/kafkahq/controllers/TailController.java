package org.kafkahq.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.sse.Event;
import io.micronaut.security.annotation.Secured;
import io.micronaut.views.View;
import io.micronaut.views.freemarker.FreemarkerViewsRenderer;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.kafkahq.configs.Role;
import org.kafkahq.repositories.RecordRepository;
import org.kafkahq.repositories.TopicRepository;
import org.reactivestreams.Publisher;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Secured(Role.ROLE_TOPIC_READ)
@Controller("${kafkahq.server.base-path:}/{cluster}/tail")
public class TailController extends AbstractController {
    private RecordRepository recordRepository;
    private TopicRepository topicRepository;
    private FreemarkerViewsRenderer freemarkerViewsRenderer;

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
    @Get
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
    @Get("sse")
    public Publisher<Event<?>> sse(
        String cluster,
        List<String> topics,
        Optional<String> search,
        Optional<List<String>> after
    ) throws ExecutionException, InterruptedException {
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
                tailBody.offsets = event.getData()
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

    @ToString
    @EqualsAndHashCode
    public static class TailBody {
        @JsonProperty("body")
        private String body;

        @JsonProperty("offsets")
        private List<String> offsets = new ArrayList<>();

    }
}
