package org.kafkahq.controllers;

import com.google.common.collect.ImmutableMap;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.views.View;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.kafkahq.configs.Role;
import org.kafkahq.models.ConsumerGroup;
import org.kafkahq.models.TopicPartition;
import org.kafkahq.modules.RequestHelper;
import org.kafkahq.repositories.ConsumerGroupRepository;
import org.kafkahq.repositories.RecordRepository;
import org.kafkahq.utils.CompletablePaged;

import javax.inject.Inject;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Secured(Role.ROLE_GROUP_READ)
@Controller("${kafkahq.server.base-path:}/{cluster}/group")
public class GroupController extends AbstractController {
    private ConsumerGroupRepository consumerGroupRepository;
    private RecordRepository recordRepository;

    @Value("${kafkahq.consumer-groups.page-size:25}")
    private Integer pageSize;

    @Inject
    public GroupController(ConsumerGroupRepository consumerGroupRepository, RecordRepository recordRepository) {
        this.consumerGroupRepository = consumerGroupRepository;
        this.recordRepository = recordRepository;
    }

    @View("groupList")
    @Get
    public HttpResponse list(HttpRequest request, String cluster, Optional<String> search, Optional<Integer> page) throws ExecutionException, InterruptedException {

        List<CompletableFuture<ConsumerGroup>> list = this.consumerGroupRepository.list(search);
        URIBuilder uri = URIBuilder.fromURI(request.getUri());

        CompletablePaged<ConsumerGroup> paged = new CompletablePaged<>(
            list,
            this.pageSize,
            uri,
            page.orElse(1)
        );

        return this.template(
            request,
            cluster,
            "search", search,
            "groups", paged.complete(),
            "pagination", ImmutableMap.builder()
                .put("size", paged.size())
                .put("before", paged.before().toNormalizedURI(false).toString())
                .put("after", paged.after().toNormalizedURI(false).toString())
                .build()
        );
    }

    @View("group")
    @Get("{groupName}")
    public HttpResponse home(HttpRequest request, String cluster, String groupName) throws ExecutionException, InterruptedException {
        return this.render(request, cluster, groupName, "topics");
    }

    @View("group")
    @Get("{groupName}/{tab:(topics|members)}")
    public HttpResponse tab(HttpRequest request, String cluster, String tab, String groupName) throws ExecutionException, InterruptedException {
        return this.render(request, cluster, groupName, tab);
    }

    private HttpResponse render(HttpRequest request, String cluster, String groupName, String tab) throws ExecutionException, InterruptedException {
        ConsumerGroup group = this.consumerGroupRepository.findByName(groupName);

        return this.template(
            request,
            cluster,
           "tab", tab,
            "group", group
        );
    }

    @Secured(Role.ROLE_GROUP_OFFSETS_UPDATE)
    @View("groupUpdate")
    @Get("{groupName}/offsets")
    public HttpResponse offsets(HttpRequest request, String cluster, String groupName) throws ExecutionException, InterruptedException {
        ConsumerGroup group = this.consumerGroupRepository.findByName(groupName);

        return this.template(
            request,
            cluster,
            "group", group
        );
    }

    @Secured(Role.ROLE_GROUP_OFFSETS_UPDATE)
    @Post(value = "{groupName}/offsets", consumes = MediaType.MULTIPART_FORM_DATA)
    public HttpResponse offsetsSubmit(HttpRequest request, String cluster, String groupName, Map<String, Long> offset) throws Throwable {
        ConsumerGroup group = this.consumerGroupRepository.findByName(groupName);

        Map<TopicPartition, Long> offsets = group.getOffsets()
            .stream()
            .map(r -> new AbstractMap.SimpleEntry<>(
                new TopicPartition(r.getTopic(), r.getPartition()),
                offset.get("offset[" + r.getTopic() + "][" + r.getPartition() + "]")
            ))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        MutableHttpResponse<Void> response = HttpResponse.redirect(request.getUri());

        this.toast(response, RequestHelper.runnableToToast(() -> this.consumerGroupRepository.updateOffsets(
                cluster,
                groupName,
                offsets
            ),
            "Offsets for '" + group.getId() + "' is updated",
            "Failed to update offsets for '" + group.getId() + "'"
        ));

        return response;
    }

    @Secured(Role.ROLE_GROUP_OFFSETS_UPDATE)
    @Get("{groupName}/offsets/start")
    public HttpResponse offsetsStart(HttpRequest request, String cluster, String groupName, String timestamp) throws ExecutionException, InterruptedException {
        ConsumerGroup group = this.consumerGroupRepository.findByName(groupName);

        List<RecordRepository.TimeOffset> offsetForTime = recordRepository.getOffsetForTime(
            cluster,
            group.getOffsets()
                .stream()
                .map(r -> new TopicPartition(r.getTopic(), r.getPartition()))
                .collect(Collectors.toList()),
            Instant.parse(timestamp).toEpochMilli()
        );

        return HttpResponse.ok(offsetForTime);
    }

    @Secured(Role.ROLE_GROUP_DELETE)
    @Get("{groupName}/delete")
    public HttpResponse delete(HttpRequest request, String cluster, String groupName) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
                this.consumerGroupRepository.delete(cluster, groupName),
            "Consumer group '" + groupName + "' is deleted",
            "Failed to consumer group " + groupName
        ));

        return response;
    }
}
