package org.akhq.repositories;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.utils.SecurityService;
import org.akhq.configs.SecurityProperties;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.akhq.models.Partition;
import org.akhq.models.Topic;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;
import org.akhq.utils.UserGroupUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Singleton
public class TopicRepository extends AbstractRepository {
    @Inject
    private AbstractKafkaWrapper kafkaWrapper;

    @Inject
    private ConsumerGroupRepository consumerGroupRepository;

    @Inject
    private LogDirRepository logDirRepository;

    @Inject
    private ConfigRepository configRepository;


    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private UserGroupUtils userGroupUtils;

    @Value("${akhq.topic.internal-regexps}")
    protected List<String> internalRegexps;

    @Value("${akhq.topic.stream-regexps}")
    protected List<String> streamRegexps;

    @Inject
    private SecurityProperties securityProperties;

    public enum TopicListView {
        ALL,
        HIDE_INTERNAL,
        HIDE_INTERNAL_STREAM,
        HIDE_STREAM,
    }

    public PagedList<Topic> list(String clusterId, Pagination pagination, TopicListView view, Optional<String> search) throws ExecutionException, InterruptedException {
        return this.list(clusterId, pagination, view, search, false);
    }

    public PagedList<Topic> list(String clusterId, Pagination pagination, TopicListView view, Optional<String> search, boolean skipConsumerGroups) throws ExecutionException, InterruptedException {
        List<String> all = all(clusterId, view, search);

        return PagedList.of(all, pagination, topicList -> this.findByName(clusterId, topicList, skipConsumerGroups));
    }

    public List<String> all(String clusterId, TopicListView view, Optional<String> search) throws ExecutionException, InterruptedException {
        ArrayList<String> list = new ArrayList<>();

        Collection<TopicListing> listTopics = kafkaWrapper.listTopics(clusterId);

        for (TopicListing item : listTopics) {
            if (isSearchMatch(search, item.name()) && isListViewMatch(view, item.name()) && isTopicMatchRegex(
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
        return this.findByName(clusterId, name, true);
    }

    public Topic findByName(String clusterId, String name, boolean skipConsumerGroups) throws ExecutionException, InterruptedException {
        Optional<Topic> topic = Optional.empty();
        if(isTopicMatchRegex(getTopicFilterRegex(),name)) {
            topic = this.findByName(clusterId, Collections.singletonList(name), skipConsumerGroups).stream().findFirst();
        }

        return topic.orElseThrow(() -> new NoSuchElementException("Topic '" + name + "' doesn't exist"));
    }

    public List<Topic> findByName(String clusterId, List<String> topics) throws ExecutionException, InterruptedException {
        return this.findByName(clusterId, topics, false);
    }

    public List<Topic> findByName(String clusterId, List<String> topics, boolean skipConsumerGroups) throws ExecutionException, InterruptedException {
        ArrayList<Topic> list = new ArrayList<>();

        Set<Map.Entry<String, TopicDescription>> topicDescriptions = kafkaWrapper.describeTopics(clusterId, topics).entrySet();
        Map<String, List<Partition.Offsets>> topicOffsets = kafkaWrapper.describeTopicsOffsets(clusterId, topics);

        Optional<List<String>> topicRegex = getTopicFilterRegex();

        for (Map.Entry<String, TopicDescription> description : topicDescriptions) {
            if(isTopicMatchRegex(topicRegex, description.getValue().name())){
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
        configRepository.updateTopic(clusterId, name, configs);
    }

    public void delete(String clusterId, String name) throws ExecutionException, InterruptedException {
        kafkaWrapper.deleteTopics(clusterId, name);
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
            userGroupUtils.getUserAttributes(Collections.singletonList(securityProperties.getDefaultGroup()))
        ));

        return Optional.of(topicFilterRegex);
    }

    @SuppressWarnings("unchecked")
    private List<String> getTopicFilterRegexFromAttributes(Map<String, Object> attributes) {
        if (attributes.get("topics-filter-regexp") != null) {
            if (attributes.get("topics-filter-regexp") instanceof List) {
                return (List<String>)attributes.get("topics-filter-regexp");
            }
        }
        return new ArrayList<>();
    }

}
