package org.akhq.controllers;

import com.google.common.collect.ImmutableMap;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.views.View;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.akhq.configs.Role;
import org.akhq.models.AccessControl;
import org.akhq.models.Consumer;
import org.akhq.models.ConsumerGroup;
import org.akhq.models.TopicPartition;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.modules.RequestHelper;
import org.akhq.repositories.AccessControlListRepository;
import org.akhq.repositories.ConsumerGroupRepository;
import org.akhq.repositories.RecordRepository;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;
import org.akhq.utils.ResultPagedList;
import org.apache.kafka.common.resource.ResourceType;
import org.codehaus.httpcache4j.uri.URIBuilder;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.inject.Inject;

@Secured(Role.ROLE_GROUP_READ)
@Controller("${akhq.server.base-path:}/")
public class GroupController extends AbstractController {
    private final AbstractKafkaWrapper kafkaWrapper;
    private final ConsumerGroupRepository consumerGroupRepository;
    private final RecordRepository recordRepository;
    private final AccessControlListRepository aclRepository;

    @Value("${akhq.pagination.page-size}")
    private Integer pageSize;

    @Inject
    public GroupController(
        AbstractKafkaWrapper kafkaWrapper,
        ConsumerGroupRepository consumerGroupRepository,
        RecordRepository recordRepository,
        AccessControlListRepository aclRepository
    ) {
        this.kafkaWrapper = kafkaWrapper;
        this.consumerGroupRepository = consumerGroupRepository;
        this.recordRepository = recordRepository;
        this.aclRepository = aclRepository;
    }

    @View("groupList")
    @Get("{cluster}/group")
    @Hidden
    public HttpResponse<?> list(HttpRequest<?> request, String cluster, Optional<String> search, Optional<Integer> page) throws ExecutionException, InterruptedException {
        URIBuilder uri = URIBuilder.fromURI(request.getUri());
        Pagination pagination = new Pagination(pageSize, uri, page.orElse(1));

        PagedList<ConsumerGroup> list = this.consumerGroupRepository.list(cluster, pagination, search);

        return this.template(
            request,
            cluster,
            "search", search,
            "groups", list,
            "pagination", ImmutableMap.builder()
                .put("size", list.total())
                .put("before", list.before().toNormalizedURI(false).toString())
                .put("after", list.after().toNormalizedURI(false).toString())
                .build()
        );
    }

    @Get("api/{cluster}/group")
    @Operation(tags = {"consumer group"}, summary = "List all consumer groups")
    public ResultPagedList<ConsumerGroup> listApi(HttpRequest<?> request, String cluster, Optional<String> search, Optional<Integer> page) throws ExecutionException, InterruptedException {
        URIBuilder uri = URIBuilder.fromURI(request.getUri());
        Pagination pagination = new Pagination(pageSize, uri, page.orElse(1));

        return ResultPagedList.of(this.consumerGroupRepository.list(cluster, pagination, search));
    }

    @View("group")
    @Get("{cluster}/group/{groupName}")
    @Hidden
    public HttpResponse<?> home(HttpRequest<?> request, String cluster, String groupName) throws ExecutionException, InterruptedException {
        return this.render(request, cluster, groupName, "topics");
    }

    @Get("api/{cluster}/group/{groupName}")
    @Operation(tags = {"consumer group"}, summary = "Retrieve a consumer group")
    public ConsumerGroup homeApi(String cluster, String groupName) throws ExecutionException, InterruptedException {
        return this.consumerGroupRepository.findByName(cluster, groupName);
    }

    @View("group")
    @Get("{cluster}/group/{groupName}/{tab:(topics|members|acls)}")
    @Hidden
    public HttpResponse<?> tab(HttpRequest<?> request, String cluster, String tab, String groupName) throws ExecutionException, InterruptedException {
        return this.render(request, cluster, groupName, tab);
    }

    private HttpResponse<?> render(HttpRequest<?> request, String cluster, String groupName, String tab) throws ExecutionException, InterruptedException {
        ConsumerGroup group = this.consumerGroupRepository.findByName(cluster, groupName);

        return this.template(
            request,
            cluster,
           "tab", tab,
            "group", group,
            "acls", aclRepository.findByResourceType(cluster, ResourceType.GROUP, groupName)
        );
    }

    @Get("api/{cluster}/group/{groupName}/offsets")
    @Operation(tags = {"consumer group"}, summary = "Retrieve a consumer group offsets")
    public List<TopicPartition.ConsumerGroupOffset> offsetsApi(String cluster, String groupName) throws ExecutionException, InterruptedException {
        return this.consumerGroupRepository.findByName(cluster, groupName).getOffsets();
    }

    @Get("api/{cluster}/group/{groupName}/members")
    @Operation(tags = {"consumer group"}, summary = "Retrieve a consumer group members")
    public List<Consumer> membersApi(String cluster, String groupName) throws ExecutionException, InterruptedException {
        return this.consumerGroupRepository.findByName(cluster, groupName).getMembers();
    }

    @Get("api/{cluster}/group/{groupName}/acls")
    @Operation(tags = {"consumer group"}, summary = "Retrieve a consumer group acls")
    public List<AccessControl> aclsApi(String cluster, String groupName) throws ExecutionException, InterruptedException {
        return aclRepository.findByResourceType(cluster, ResourceType.GROUP, groupName);
    }

    @Secured(Role.ROLE_GROUP_OFFSETS_UPDATE)
    @View("groupUpdate")
    @Get("{cluster}/group/{groupName}/offsets")
    @Hidden
    public HttpResponse<?> offsets(HttpRequest<?> request, String cluster, String groupName) throws ExecutionException, InterruptedException {
        ConsumerGroup group = this.consumerGroupRepository.findByName(cluster, groupName);

        return this.template(
            request,
            cluster,
            "group", group
        );
    }

    @Secured(Role.ROLE_GROUP_OFFSETS_UPDATE)
    @Post(value = "{cluster}/group/{groupName}/offsets", consumes = MediaType.MULTIPART_FORM_DATA)
    @Hidden
    public HttpResponse<?> offsetsSubmit(HttpRequest<?> request, String cluster, String groupName, Map<String, Long> offset) throws Throwable {
        ConsumerGroup group = this.consumerGroupRepository.findByName(cluster, groupName);

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
    @Post(value = "api/{cluster}/group/{groupName}/offsets", consumes = MediaType.APPLICATION_JSON)
    @Operation(tags = {"consumer group"}, summary = "Update consumer group offsets")
    public HttpResponse<?> offsetsApi(
        String cluster,
        String groupName,
        @Body List<OffsetsUpdate> offsets
    ) {
        this.consumerGroupRepository.updateOffsets(
            cluster,
            groupName,
            offsets
                .stream()
                .map(r -> new AbstractMap.SimpleEntry<>(
                        new TopicPartition(r.getTopic(), r.getPartition()),
                        r.getOffset()
                    )
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );

        return HttpResponse.noContent();
    }

    @Secured(Role.ROLE_GROUP_OFFSETS_UPDATE)
    @Get("{cluster}/group/{groupName}/offsets/start")
    @Hidden
    public HttpResponse<?> offsetsStart(HttpRequest<?> request, String cluster, String groupName, Instant timestamp) throws ExecutionException, InterruptedException {
        return HttpResponse.ok(this.offsetsStartApi(cluster, groupName, timestamp));
    }

    @Secured(Role.ROLE_GROUP_OFFSETS_UPDATE)
    @Get("api/{cluster}/group/{groupName}/offsets/start")
    @Operation(tags = {"consumer group"}, summary = "Retrive consumer group offsets by timestamp")
    public List<RecordRepository.TimeOffset> offsetsStartApi(String cluster, String groupName, Instant timestamp) throws ExecutionException, InterruptedException {
        ConsumerGroup group = this.consumerGroupRepository.findByName(cluster, groupName);

        return recordRepository.getOffsetForTime(
            cluster,
            group.getOffsets()
                .stream()
                .map(r -> new TopicPartition(r.getTopic(), r.getPartition()))
                .collect(Collectors.toList()),
            timestamp.toEpochMilli()
        );
    }

    @Secured(Role.ROLE_GROUP_DELETE)
    @Get("{cluster}/group/{groupName}/delete")
    @Hidden
    public HttpResponse<?> delete(HttpRequest<?> request, String cluster, String groupName) {
        MutableHttpResponse<Void> response = HttpResponse.ok();

        this.toast(response, RequestHelper.runnableToToast(() ->
                this.kafkaWrapper.deleteConsumerGroups(cluster, groupName),
            "Consumer group '" + groupName + "' is deleted",
            "Failed to consumer group " + groupName
        ));

        return response;
    }

    @Secured(Role.ROLE_GROUP_DELETE)
    @Delete("api/{cluster}/group/{groupName}")
    @Operation(tags = {"consumer group"}, summary = "Delete a consumer group")
    public HttpResponse<?> deleteApi(String cluster, String groupName) throws ExecutionException, InterruptedException {
        this.kafkaWrapper.deleteConsumerGroups(cluster, groupName);

        return HttpResponse.noContent();
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class OffsetsUpdate {
        private String topic;
        private int partition;
        private long offset;
    }
}
