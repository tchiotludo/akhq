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
import java.util.Optional;
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
    public View list(Request request, String cluster, Optional<String> search) throws ExecutionException, InterruptedException {
        return this.template(
            request,
            cluster,
            Results
                .html("groupList")
                .put("search", search)
                .put("groups", this.consumerGroupRepository.list(search))
        );
    }

    @GET
    @Path("{groupName}")
    public View home(Request request, String cluster, String groupName) throws ExecutionException, InterruptedException {
        return this.render(request, cluster, groupName, "topics");
    }

    @GET
    @Path("{groupName}/{tab:(topics|members)}")
    public View tab(Request request, String cluster, String tab, String groupName) throws ExecutionException, InterruptedException {
        return this.render(request, cluster, groupName, tab);
    }

    public View render(Request request, String cluster, String groupName, String tab) throws ExecutionException, InterruptedException {
        ConsumerGroup group = this.consumerGroupRepository.findByName(groupName);

        return this.template(
            request,
            cluster,
            Results
                .html("group")
                .put("tab", tab)
                .put("group", group)
        );
    }

    @GET
    @Path("{groupName}/offsets")
    public View offsets(Request request, String cluster, String groupName) throws ExecutionException, InterruptedException {
        ConsumerGroup group = this.consumerGroupRepository.findByName(groupName);

        return this.template(
            request,
            cluster,
            Results
                .html("groupUpdate")
                .put("group", group)
        );
    }

    @POST
    @Path("{groupName}/offsets")
    public void offsetsSubmit(Request request, Response response, String cluster, String groupName) throws Throwable {
        ConsumerGroup group = this.consumerGroupRepository.findByName(groupName);

        Map<TopicPartition, Long> offsets = group.getOffsets()
            .stream()
            .map(r -> new AbstractMap.SimpleEntry<>(
                new TopicPartition(r.getTopic(), r.getPartition()),
                request.param("offset[" + r.getTopic() + "][" + r.getPartition() + "]").longValue()
            ))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        this.toast(request, RequestHelper.runnableToToast(() -> this.consumerGroupRepository.updateOffsets(
                cluster,
                groupName,
                offsets
            ),
            "Offsets for '" + group.getId() + "' is updated",
            "Failed to update offsets for '" + group.getId() + "'"
        ));

        response.redirect(request.path());
    }

    @GET
    @Path("{groupName}/offsets/start")
    public Result offsetsStart(Request request, String cluster, String groupName, String timestamp) throws ExecutionException, InterruptedException {
        ConsumerGroup group = this.consumerGroupRepository.findByName(groupName);

        List<RecordRepository.TimeOffset> offsetForTime = recordRepository.getOffsetForTime(
            cluster,
            group.getOffsets()
                .stream()
                .map(r -> new TopicPartition(r.getTopic(), r.getPartition()))
                .collect(Collectors.toList()),
            Instant.parse(timestamp).toEpochMilli()
        );

        return Results
            .with(offsetForTime)
            .type(MediaType.json);
    }

    @GET
    @Path("{groupName}/delete")
    public Result delete(Request request, String cluster, String groupName) {
        this.toast(request, RequestHelper.runnableToToast(() ->
                this.consumerGroupRepository.delete(cluster, groupName),
            "Consumer group '" + groupName + "' is deleted",
            "Failed to consumer group " + groupName
        ));

        return Results.ok();
    }
}
