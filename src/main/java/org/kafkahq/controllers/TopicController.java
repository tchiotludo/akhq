package org.kafkahq.controllers;

import com.google.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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

import java.util.Collections;
import java.util.List;
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
    public View home(Request request) throws ExecutionException, InterruptedException {
        Topic topic = this.topicRepository.findByName(request.param("id").value());
        List<ConsumerRecord<String, String>> data = this.recordRepository.consume(
            request.param("cluster").value(),
            Collections.singletonList(topic.getName()),
            new RecordRepository.Options()
        );

        return this.template(
            request,
            Results
                .html("topic")
                .put("tab", "data")
                .put("topic", topic)
                .put("datas", data)
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
