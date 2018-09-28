package org.kafkahq.controllers;

import com.google.inject.Inject;
import org.jooby.Request;
import org.jooby.Result;
import org.jooby.Results;
import org.jooby.View;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.kafkahq.models.ConsumerGroup;
import org.kafkahq.repositories.ConsumerGroupRepository;
import org.kafkahq.response.ResultStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

@Path("/{cluster}/group")
public class GroupController extends AbstractController {
    private static final Logger logger = LoggerFactory.getLogger(TopicController.class);

    @Inject
    private ConsumerGroupRepository consumerGroupRepository;

    @GET
    public View list(Request request) throws ExecutionException, InterruptedException {
        return this.template(
            request,
            Results
                .html("groupList")
                .put("groups", this.consumerGroupRepository.list())
        );
    }

    @GET
    @Path("{id}")
    public View home(Request request) throws ExecutionException, InterruptedException {
        return this.group(request, "topics");
    }

    @GET
    @Path("{id}/{tab:(topics|members)}")
    public View tab(Request request) throws ExecutionException, InterruptedException {
        return this.group(request, request.param("tab").value());
    }

    public View group(Request request, String tab) throws ExecutionException, InterruptedException {
        ConsumerGroup group = this.consumerGroupRepository.findByName(request.param("id").value());

        return this.template(
            request,
            Results
                .html("group")
                .put("tab", tab)
                .put("group", group)
        );
    }


    @GET
    @Path("{id}/delete")
    public Result delete(Request request) {
        String name = request.param("id").value();
        ResultStatusResponse result = new ResultStatusResponse();

        try {
            this.consumerGroupRepository.delete(request.param("cluster").value(), name);

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
