package org.akhq.repositories;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.retry.annotation.Retryable;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.akhq.configs.Connection;
import org.akhq.models.Partition;
import org.akhq.models.Topic;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.modules.KafkaModule;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

@Singleton
@Slf4j
public class TopicRepository extends AbstractRepository {
    @Inject
    private AbstractKafkaWrapper kafkaWrapper;

    @Inject
    private KafkaModule kafkaModule;

    @Inject
    private LogDirRepository logDirRepository;

    @Inject
    private ConfigRepository configRepository;


    @Inject
    private ApplicationContext applicationContext;

    @Value("${akhq.topic.internal-regexps}")
    protected List<String> internalRegexps;

    @Value("${akhq.topic.stream-regexps}")
    protected List<String> streamRegexps;

    public enum TopicListView {
        ALL,
        HIDE_INTERNAL,
        HIDE_INTERNAL_STREAM,
        HIDE_STREAM,
    }

    public enum TopicGroupsListView {
        ALL,
        HIDE_EMPTY
    }

    public PagedList<Topic> list(String clusterId, Pagination pagination, TopicListView view, Optional<String> search, List<String> filters) throws ExecutionException, InterruptedException {
        TopicCandidates candidates = topicCandidates(clusterId, view, search, filters);

        if (candidates.allowlistActive()) {
            AllowlistedTopicDescriptions descriptions = describeExistingAllowlistedTopics(clusterId, candidates.names());

            return PagedList.of(
                descriptions.names(),
                pagination,
                topicList -> this.findAllowlistedTopics(clusterId, topicList, descriptions.descriptions())
            );
        }

        return PagedList.of(candidates.names(), pagination, topicList -> this.findByName(clusterId, topicList));
    }

    public List<String> all(String clusterId, TopicListView view, Optional<String> search, List<String> filters) throws ExecutionException, InterruptedException {
        TopicCandidates candidates = topicCandidates(clusterId, view, search, filters);

        if (candidates.allowlistActive()) {
            return describeExistingAllowlistedTopics(clusterId, candidates.names()).names();
        }

        return candidates.names();
    }

    private TopicCandidates topicCandidates(String clusterId, TopicListView view, Optional<String> search, List<String> filters) throws ExecutionException {
        List<String> topicDiscoveryAllowlist = topicDiscoveryAllowlist(clusterId);

        List<String> names = topicDiscoveryAllowlist.isEmpty() ?
            kafkaWrapper.listTopics(clusterId)
                .stream()
                .map(TopicListing::name)
                .collect(Collectors.toList()) :
            topicDiscoveryAllowlist;

        return new TopicCandidates(
            !topicDiscoveryAllowlist.isEmpty(),
            names
                .stream()
                .filter(name -> isSearchMatch(search, name) && isMatchRegex(filters, name))
                .filter(name -> isListViewMatch(view, name))
                .sorted(Comparator.comparing(String::toLowerCase))
                .collect(Collectors.toList())
        );
    }

    private List<String> topicDiscoveryAllowlist(String clusterId) {
        Connection.TopicDiscovery topicDiscovery = kafkaModule.getConnection(clusterId).getTopicDiscovery();

        if (topicDiscovery == null || topicDiscovery.getAllowlist() == null) {
            return Collections.emptyList();
        }

        return topicDiscovery.getAllowlist()
            .stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .distinct()
            .collect(Collectors.toList());
    }

    public boolean isListViewMatch(TopicListView view, String value) {
        switch (view) {
            case HIDE_STREAM:
                return !isStream(value);
            case HIDE_INTERNAL:
                return !isInternal(value);
            case HIDE_INTERNAL_STREAM:
                return !isInternal(value) && !isStream(value);
        }

        return true;
    }

    public Topic findByName(String clusterId, String name) throws ExecutionException, InterruptedException {
        return this.findByName(clusterId, Collections.singletonList(name))
            .stream()
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Topic '" + name + "' doesn't exist"));
    }

    public List<Topic> findByName(String clusterId, List<String> topics) throws ExecutionException, InterruptedException {
        Set<Map.Entry<String, TopicDescription>> topicDescriptions = kafkaWrapper.describeTopics(clusterId, topics).entrySet();
        Map<String, List<Partition.Offsets>> topicOffsets = kafkaWrapper.describeTopicsOffsets(clusterId, topics);

        return buildTopics(clusterId, topicDescriptions, topicOffsets);
    }

    private List<Topic> findAllowlistedTopics(String clusterId, List<String> topics, Map<String, TopicDescription> topicDescriptions) throws ExecutionException, InterruptedException {
        List<String> existingTopics = topics.stream()
            .filter(topicDescriptions::containsKey)
            .collect(Collectors.toList());
        Map<String, List<Partition.Offsets>> topicOffsets = existingTopics.isEmpty() ?
            Collections.emptyMap() :
            kafkaWrapper.describeTopicsOffsets(clusterId, existingTopics);
        Set<Map.Entry<String, TopicDescription>> descriptions = existingTopics.stream()
            .map(topic -> new AbstractMap.SimpleEntry<>(topic, topicDescriptions.get(topic)))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        return buildTopics(clusterId, descriptions, topicOffsets);
    }

    private AllowlistedTopicDescriptions describeExistingAllowlistedTopics(String clusterId, List<String> topics) throws ExecutionException, InterruptedException {
        Map<String, TopicDescription> topicDescriptions = kafkaWrapper.describeExistingTopics(clusterId, topics);

        topics.stream()
            .filter(topic -> !topicDescriptions.containsKey(topic))
            .forEach(topic -> log.warn("Topic configured in topic-discovery.allowlist does not exist for cluster {}: {}", clusterId, topic));

        List<String> existingNames = topics.stream()
            .filter(topicDescriptions::containsKey)
            .collect(Collectors.toList());

        return new AllowlistedTopicDescriptions(existingNames, topicDescriptions);
    }

    private List<Topic> buildTopics(
        String clusterId,
        Set<Map.Entry<String, TopicDescription>> topicDescriptions,
        Map<String, List<Partition.Offsets>> topicOffsets
    ) throws ExecutionException, InterruptedException {
        ArrayList<Topic> list = new ArrayList<>();

        for (Map.Entry<String, TopicDescription> description : topicDescriptions) {
            list.add(
                new Topic(
                    description.getValue(),
                    logDirRepository.findByTopic(clusterId, description.getValue().name()),
                    topicOffsets.get(description.getValue().name()),
                    isInternal(description.getValue().name()),
                    isStream(description.getValue().name())
                )
            );
        }

        list.sort(Comparator.comparing(Topic::getName));

        return list;
    }

    private static class TopicCandidates {
        private final boolean allowlistActive;
        private final List<String> names;

        private TopicCandidates(boolean allowlistActive, List<String> names) {
            this.allowlistActive = allowlistActive;
            this.names = names;
        }

        private boolean allowlistActive() {
            return allowlistActive;
        }

        private List<String> names() {
            return names;
        }
    }

    private static class AllowlistedTopicDescriptions {
        private final List<String> names;
        private final Map<String, TopicDescription> descriptions;

        private AllowlistedTopicDescriptions(List<String> names, Map<String, TopicDescription> descriptions) {
            this.names = names;
            this.descriptions = descriptions;
        }

        private List<String> names() {
            return names;
        }

        private Map<String, TopicDescription> descriptions() {
            return descriptions;
        }
    }

    private boolean isInternal(String name) {
        return this.internalRegexps
            .stream()
            .anyMatch(name::matches);
    }

    private boolean isStream(String name) {
        return this.streamRegexps
            .stream()
            .anyMatch(name::matches);
    }

    public void create(String clusterId, String name, int partitions, short replicationFactor, List<org.akhq.models.Config> configs) throws ExecutionException, InterruptedException {
        kafkaWrapper.createTopics(clusterId, name, partitions, replicationFactor, configs);
    }

    public void delete(String clusterId, String name) throws ExecutionException, InterruptedException {
        kafkaWrapper.deleteTopics(clusterId, name);
    }

    public void increasePartition(String clusterId, String name, int partitions) throws ExecutionException, InterruptedException {
        kafkaWrapper.alterTopicPartition(clusterId, name, partitions);
    }

    @Retryable(
        includes = {
            UnknownTopicOrPartitionException.class
        }, delay = "${akhq.topic.retry.topic-exists.delay:3s}")
    void checkIfTopicExists(String clusterId, String name) throws ExecutionException {
        kafkaWrapper.describeTopics(clusterId, Collections.singletonList(name));
    }
}
