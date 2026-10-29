package org.akhq.mcp.services;

import jakarta.inject.Singleton;
import org.akhq.mcp.model.GetConsumerGroupMembersResult;
import org.akhq.mcp.model.GetConsumerGroupOffsetsResult;
import org.akhq.mcp.model.GetConsumerGroupResult;
import org.akhq.mcp.model.SearchConsumerGroupsResult;
import org.akhq.models.Consumer;
import org.akhq.models.ConsumerGroup;
import org.akhq.models.TopicPartition;
import org.akhq.repositories.ConsumerGroupRepository;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class AkhqConsumerGroupToolService {
    private final ConsumerGroupRepository repository;

    public AkhqConsumerGroupToolService(ConsumerGroupRepository repository) {
        this.repository = repository;
    }

    public SearchConsumerGroupsResult searchConsumerGroups(String cluster, List<String> filters, String search, Integer maxResults) {
        int limit = McpLimits.maxResults(maxResults);
        List<String> names = repository.all(cluster, McpLimits.search(search), filters);
        List<String> returned = names.subList(0, Math.min(limit, names.size()));
        boolean truncated = names.size() > returned.size();

        List<SearchConsumerGroupsResult.ConsumerGroupOverview> groups = returned.isEmpty()
            ? List.of()
            : repository.findByName(cluster, returned, filters).stream()
                .sorted(Comparator.comparing(ConsumerGroup::getId, String::compareToIgnoreCase))
                .map(group -> new SearchConsumerGroupsResult.ConsumerGroupOverview(
                    group.getId(),
                    String.valueOf(group.getState()),
                    group.getPartitionAssignor(),
                    group.getMembers().size(),
                    group.getTopics(),
                    totalLag(group)
                ))
                .toList();

        return new SearchConsumerGroupsResult(
            cluster, names.size(), truncated, groups,
            McpLimits.listMessage("consumer group", names.size(), groups.size(), truncated)
        );
    }

    public GetConsumerGroupResult getConsumerGroup(String cluster, List<String> filters, String name)
        throws ExecutionException, InterruptedException {
        ConsumerGroup group = find(cluster, filters, name);
        var coordinator = group.getCoordinator();

        return new GetConsumerGroupResult(
            cluster,
            group.getId(),
            group.isSimpleConsumerGroup(),
            String.valueOf(group.getState()),
            group.getPartitionAssignor(),
            coordinator == null ? null : new GetConsumerGroupResult.Coordinator(
                coordinator.getId(), coordinator.getHost(), coordinator.getPort(), coordinator.getRack()
            ),
            group.getMembers().size(),
            totalLag(group),
            group.getTopics().stream()
                .map(topic -> new GetConsumerGroupResult.TopicLag(topic, group.isActiveTopic(topic), group.getOffsetLag(topic)))
                .toList()
        );
    }

    public GetConsumerGroupOffsetsResult getOffsets(String cluster, List<String> filters, String name, String topic, Integer maxResults)
        throws ExecutionException, InterruptedException {
        int limit = McpLimits.maxItems(maxResults);
        String topicFilter = topic == null || topic.isBlank() ? null : topic.trim();
        List<TopicPartition.ConsumerGroupOffset> offsets = find(cluster, filters, name).getOffsets().stream()
            .filter(offset -> topicFilter == null || offset.getTopic().equals(topicFilter))
            .toList();
        List<GetConsumerGroupOffsetsResult.PartitionOffset> returned = offsets.stream()
            .limit(limit)
            .map(offset -> new GetConsumerGroupOffsetsResult.PartitionOffset(
                offset.getTopic(),
                offset.getPartition(),
                offset.getOffset().orElse(null),
                offset.getFirstOffset().orElse(null),
                offset.getLastOffset().orElse(null),
                offset.getOffsetLag().orElse(null)
            ))
            .toList();
        boolean truncated = offsets.size() > returned.size();

        return new GetConsumerGroupOffsetsResult(
            cluster, name.trim(), offsets.size(), truncated, returned,
            truncated
                ? "Found " + offsets.size() + " partition offsets, returning the first " + returned.size()
                    + " sorted by topic and partition. Set `topic` or raise `maxResults` to see the others."
                : "Found " + offsets.size() + " partition offset(s)."
        );
    }

    public GetConsumerGroupMembersResult getMembers(String cluster, List<String> filters, String name, Integer maxResults)
        throws ExecutionException, InterruptedException {
        int limit = McpLimits.maxResults(maxResults);
        List<Consumer> members = find(cluster, filters, name).getMembers();
        List<GetConsumerGroupMembersResult.Member> returned = members.stream()
            .limit(limit)
            .map(member -> new GetConsumerGroupMembersResult.Member(
                member.getId(),
                member.getClientId(),
                member.getHost(),
                member.getAssignments().stream()
                    .collect(Collectors.groupingBy(TopicPartition::getTopic))
                    .entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .map(entry -> new GetConsumerGroupMembersResult.Assignment(
                        entry.getKey(),
                        entry.getValue().stream().map(TopicPartition::getPartition).sorted().toList()
                    ))
                    .toList()
            ))
            .toList();
        boolean truncated = members.size() > returned.size();

        return new GetConsumerGroupMembersResult(
            cluster, name.trim(), members.size(), truncated, returned,
            members.isEmpty()
                ? "The consumer group has no active member."
                : truncated
                    ? "Found " + members.size() + " members, returning the first " + returned.size() + "."
                    : "Found " + members.size() + " member(s)."
        );
    }

    private ConsumerGroup find(String cluster, List<String> filters, String name)
        throws ExecutionException, InterruptedException {
        try {
            return repository.findByName(cluster, name, filters);
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("Consumer group '" + name + "' doesn't exist", e);
        }
    }

    private static long totalLag(ConsumerGroup group) {
        return group.getOffsets().stream()
            .map(TopicPartition.ConsumerGroupOffset::getOffsetLag)
            .mapToLong(lag -> lag.orElse(0L))
            .sum();
    }
}
