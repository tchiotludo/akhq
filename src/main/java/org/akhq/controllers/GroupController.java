package org.akhq.controllers;

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
import org.apache.kafka.common.resource.ResourceType;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.akhq.configs.Role;
import org.akhq.models.ConsumerGroup;
import org.akhq.models.TopicPartition;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.modules.RequestHelper;
import org.akhq.repositories.AccessControlListRepository;
import org.akhq.repositories.ConsumerGroupRepository;
import org.akhq.repositories.RecordRepository;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;

import javax.inject.Inject;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Secured(Role.ROLE_GROUP_READ)
@Controller("${akhq.server.base-path:}/{cluster}/group")
public class GroupController extends AbstractController {
    private AbstractKafkaWrapper kafkaWrapper;
    private ConsumerGroupRepository consumerGroupRepository;
    private RecordRepository recordRepository;
    private AccessControlListRepository aclRepository;

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
    @Get
    public HttpResponse list(HttpRequest request, String cluster, Optional<String> search, Optional<Integer> page) throws ExecutionException, InterruptedException {
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

    @View("group")
    @Get("{groupName}")
    public HttpResponse home(HttpRequest request, String cluster, String groupName) throws ExecutionException, InterruptedException {
        return this.render(request, cluster, groupName, "topics");
    }

    @View("group")
    @Get("{groupName}/{tab:(topics|members|acls)}")
    public HttpResponse tab(HttpRequest request, String cluster, String tab, String groupName) throws ExecutionException, InterruptedException {
        return this.render(request, cluster, groupName, tab);
    }

    private HttpResponse render(HttpRequest request, String cluster, String groupName, String tab) throws ExecutionException, InterruptedException {
        ConsumerGroup group = this.consumerGroupRepository.findByName(cluster, groupName);

        return this.template(
            request,
            cluster,
           "tab", tab,
            "group", group,
            "acls", aclRepository.findByResourceType(cluster, ResourceType.GROUP, groupName)
        );
    }

    @Secured(Role.ROLE_GROUP_OFFSETS_UPDATE)
    @View("groupUpdate")
    @Get("{groupName}/offsets")
    public HttpResponse offsets(HttpRequest request, String cluster, String groupName) throws ExecutionException, InterruptedException {
        ConsumerGroup group = this.consumerGroupRepository.findByName(cluster, groupName);

        return this.template(
            request,
            cluster,
            "group", group
        );
    }

    @Secured(Role.ROLE_GROUP_OFFSETS_UPDATE)
    @Post(value = "{groupName}/offsets", consumes = MediaType.MULTIPART_FORM_DATA)
    public HttpResponse offsetsSubmit(HttpRequest request, String cluster, String groupName, Map<String, Long> offset) throws Throwable {
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
    @Get("{groupName}/offsets/start")
    public HttpResponse offsetsStart(HttpRequest request, String cluster, String groupName, String timestamp) throws ExecutionException, InterruptedException {
        ConsumerGroup group = this.consumerGroupRepository.findByName(cluster, groupName);

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
                this.kafkaWrapper.deleteConsumerGroups(cluster, groupName),
            "Consumer group '" + groupName + "' is deleted",
            "Failed to consumer group " + groupName
        ));

        return response;
    }
}
