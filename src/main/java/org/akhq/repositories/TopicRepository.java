package org.akhq.repositories;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.retry.annotation.Retryable;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.utils.SecurityService;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.akhq.models.Partition;
import org.akhq.models.Topic;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

@Singleton
public class TopicRepository extends AbstractRepository {
    @Inject
    private AbstractKafkaWrapper kafkaWrapper;

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

    public PagedList<Topic> list(String clusterId, Pagination pagination, TopicListView view, Optional<String> search, List<String> filters) throws ExecutionException, InterruptedException {
        List<String> all = all(clusterId, view, search, filters);

        return PagedList.of(all, pagination, topicList -> this.findByName(clusterId, topicList));
    }

    public List<String> all(String clusterId, TopicListView view, Optional<String> search, List<String> filters) throws ExecutionException, InterruptedException {
        return kafkaWrapper.listTopics(clusterId)
            .stream()
            .map(TopicListing::name)
            .filter(name -> isSearchMatch(search, name) && isMatchRegex(filters, name))
            .filter(name -> isListViewMatch(view, name))
            .sorted(Comparator.comparing(String::toLowerCase))
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
        ArrayList<Topic> list = new ArrayList<>();
        Set<Map.Entry<String, TopicDescription>> topicDescriptions = kafkaWrapper.describeTopics(clusterId, topics).entrySet();
        Map<String, List<Partition.Offsets>> topicOffsets = kafkaWrapper.describeTopicsOffsets(clusterId, topics);

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

    @Retryable(
        includes = {
            UnknownTopicOrPartitionException.class
        }, delay = "${akhq.topic.retry.topic-exists.delay:3s}")
    void checkIfTopicExists(String clusterId, String name) throws ExecutionException {
        kafkaWrapper.describeTopics(clusterId, Collections.singletonList(name));
    }
}
