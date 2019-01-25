package org.kafkahq.controllers;

import com.google.inject.Inject;
import org.jooby.*;
import org.jooby.mvc.GET;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.kafkahq.models.ConsumerGroup;
import org.kafkahq.models.TopicPartition;
import org.kafkahq.modules.RequestHelper;
import org.kafkahq.repositories.ConsumerGroupRepository;
import org.kafkahq.repositories.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Path("/{cluster}/group")
public class GroupController extends AbstractController {
    private static final Logger logger = LoggerFactory.getLogger(TopicController.class);

    @Inject
    private ConsumerGroupRepository consumerGroupRepository;

    @Inject
    private RecordRepository recordRepository;

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
    @Path("{id}/offsets")
    public View offsets(Request request) throws ExecutionException, InterruptedException {
        ConsumerGroup group = this.consumerGroupRepository.findByName(request.param("id").value());

        return this.template(
            request,
            Results
                .html("groupUpdate")
                .put("group", group)
        );
    }

    @POST
    @Path("{id}/offsets")
    public void offsetsSubmit(Request request, Response response) throws Throwable {
        ConsumerGroup group = this.consumerGroupRepository.findByName(request.param("id").value());

        Map<TopicPartition, Long> offsets = group.getOffsets()
            .stream()
            .map(r -> new AbstractMap.SimpleEntry<>(
                new TopicPartition(r.getTopic(), r.getPartition()),
                request.param("offset[" + r.getTopic() + "][" + r.getPartition() + "]").longValue()
            ))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        this.toast(request, RequestHelper.runnableToToast(() -> this.consumerGroupRepository.updateOffsets(
                request.param("cluster").value(),
                request.param("id").value(),
                offsets
            ),
            "Offsets for '" + group.getId() + "' is updated",
            "Failed to update offsets for '" + group.getId() + "'"
        ));

        response.redirect(request.path());
    }

    @GET
    @Path("{id}/offsets/start")
    public Result offsetsStart(Request request) throws ExecutionException, InterruptedException {
        ConsumerGroup group = this.consumerGroupRepository.findByName(request.param("id").value());

        List<RecordRepository.TimeOffset> offsetForTime = recordRepository.getOffsetForTime(
            request.param("cluster").value(),
            group.getOffsets()
                .stream()
                .map(r -> new TopicPartition(r.getTopic(), r.getPartition()))
                .collect(Collectors.toList()),
            Instant.parse(request.param("timestamp").value()).toEpochMilli()
        );

        return Results
            .with(offsetForTime)
            .type(MediaType.json);
    }

    @GET
    @Path("{id}/delete")
    public Result delete(Request request, String id) {
        this.toast(request, RequestHelper.runnableToToast(() ->
                this.consumerGroupRepository.delete(request.param("cluster").value(), id),
            "Consumer group '" + id + "' is deleted",
            "Failed to consumer group " + id
        ));

        return Results.ok();
    }
}
