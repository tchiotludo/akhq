package org.kafkahq.controllers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.codehaus.httpcache4j.uri.QueryParams;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.jooby.Request;
import org.jooby.Result;
import org.jooby.Results;
import org.jooby.View;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.kafkahq.models.Topic;
import org.kafkahq.repositories.RecordRepository;
import org.kafkahq.repositories.TopicRepository;
import org.kafkahq.response.ResultStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Path("/{cluster}/topic")
public class TopicController extends AbstractController {
    private static final Logger logger = LoggerFactory.getLogger(TopicController.class);

    @Inject
    private TopicRepository topicRepository;

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
    @Path("{id}")
    public View home(Request request) throws ExecutionException, InterruptedException, URISyntaxException {
        Topic topic = this.topicRepository.findByName(request.param("id").value());

        RecordRepository.Options options = new RecordRepository.Options(
            request.param("cluster").value(),
            topic.getName()
        );

        request.param("after").toOptional().ifPresent(options::setAfter);
        request.param("partition").toOptional(Integer.class).ifPresent(options::setPartition);
        request.param("sort").toOptional(RecordRepository.Options.Sort.class).ifPresent(options::setSort);

        List<ConsumerRecord<String, String>> data = this.recordRepository.consume(options);

        URIBuilder uri = URIBuilder.empty()
            .withPath(request.path())
            .withParameters(QueryParams.parse(request.queryString().orElse("")));

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
    @Path("{id}/{tab:(partitions|groups)}")
    public View tab(Request request) throws ExecutionException, InterruptedException {
        return this.topic(request, request.param("tab").value());
    }

    public View topic(Request request, String tab) throws ExecutionException, InterruptedException {
        Topic topic = this.topicRepository.findByName(request.param("id").value());

        return this.template(
            request,
            Results
                .html("topic")
                .put("tab", tab)
                .put("topic", topic)
        );
    }

    @GET
    @Path("{id}/delete")
    public Result delete(Request request) {
        String name = request.param("id").value();
        ResultStatusResponse result = new ResultStatusResponse();

        try {
            this.topicRepository.delete(request.param("cluster").value(), name);

            result.result = true;
            result.message = "Topic '" + name + "' is deleted";

            return Results.with(result, 200);
        } catch (Exception exception) {
            logger.error("Failed to delete topic " + name, exception);

            result.result = false;
            result.message = exception.getCause().getMessage();

            return Results.with(result, 500);
        }
    }
}
