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
import org.akhq.utils.DefaultGroupUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
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

    @Inject
    private DefaultGroupUtils defaultGroupUtils;

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

    public PagedList<Topic> list(String clusterId, Pagination pagination, TopicListView view, Optional<String> search) throws ExecutionException, InterruptedException {
        List<String> all = all(clusterId, view, search);

        return PagedList.of(all, pagination, topicList -> this.findByName(clusterId, topicList));
    }

    public List<String> all(String clusterId, TopicListView view, Optional<String> search) throws ExecutionException, InterruptedException {
        ArrayList<String> list = new ArrayList<>();

        Collection<TopicListing> listTopics = kafkaWrapper.listTopics(clusterId);

        for (TopicListing item : listTopics) {
            if (isSearchMatch(search, item.name()) && isListViewMatch(view, item.name()) && isMatchRegex(
                getTopicFilterRegex(), item.name())) {
                list.add(item.name());
            }
        }

        list.sort(Comparator.comparing(String::toLowerCase));

        return list;
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
        Optional<Topic> topic = Optional.empty();
        if(isMatchRegex(getTopicFilterRegex(),name)) {
            topic = this.findByName(clusterId, Collections.singletonList(name)).stream().findFirst();
        }

        return topic.orElseThrow(() -> new NoSuchElementException("Topic '" + name + "' doesn't exist"));
    }

    public List<Topic> findByName(String clusterId, List<String> topics) throws ExecutionException, InterruptedException {
        ArrayList<Topic> list = new ArrayList<>();
        Optional<List<String>> topicRegex = getTopicFilterRegex();

        List<String> filteredTopics = topics.stream()
                .filter(t -> isMatchRegex(topicRegex, t))
                .collect(Collectors.toList());
        Set<Map.Entry<String, TopicDescription>> topicDescriptions = kafkaWrapper.describeTopics(clusterId, filteredTopics).entrySet();
        Map<String, List<Partition.Offsets>> topicOffsets = kafkaWrapper.describeTopicsOffsets(clusterId, filteredTopics);

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
        kafkaWrapper.createTopics(clusterId, name, partitions, replicationFactor);
        checkIfTopicExists(clusterId, name);
        configRepository.updateTopic(clusterId, name, configs);
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

    private Optional<List<String>> getTopicFilterRegex() {

        List<String> topicFilterRegex = new ArrayList<>();

        if (applicationContext.containsBean(SecurityService.class)) {
            SecurityService securityService = applicationContext.getBean(SecurityService.class);
            Optional<Authentication> authentication = securityService.getAuthentication();
            if (authentication.isPresent()) {
                Authentication auth = authentication.get();
                topicFilterRegex.addAll(getTopicFilterRegexFromAttributes(auth.getAttributes()));
            }
        }
        // get topic filter regex for default groups
        topicFilterRegex.addAll(getTopicFilterRegexFromAttributes(
            defaultGroupUtils.getDefaultAttributes()
        ));

        return Optional.of(topicFilterRegex);
    }

    @SuppressWarnings("unchecked")
    private List<String> getTopicFilterRegexFromAttributes(Map<String, Object> attributes) {
        if ((attributes.get("topicsFilterRegexp") != null) && (attributes.get("topicsFilterRegexp") instanceof List)) {
		    return (List<String>)attributes.get("topicsFilterRegexp");
		}
        return new ArrayList<>();
    }

}
