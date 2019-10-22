package org.kafkahq.repositories;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.utils.SecurityService;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.kafkahq.models.ConsumerGroup;
import org.kafkahq.models.Partition;
import org.kafkahq.models.Topic;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.modules.KafkaWrapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class TopicRepository extends AbstractRepository {
    @Inject
    KafkaWrapper kafkaWrapper;

    @Inject
    private KafkaModule kafkaModule;

    @Inject
    private ConsumerGroupRepository consumerGroupRepository;

    @Inject
    private LogDirRepository logDirRepository;

    @Inject
    private ConfigRepository configRepository;

    @Inject
    ApplicationContext applicationContext;

    @Value("${kafkahq.topic.internal-regexps}")
    protected List<String> internalRegexps;

    @Value("${kafkahq.topic.stream-regexps}")
    protected List<String> streamRegexps;

    public enum TopicListView {
        ALL,
        HIDE_INTERNAL,
        HIDE_INTERNAL_STREAM,
        HIDE_STREAM,
    }

    public List<CompletableFuture<Topic>> list(String clusterId, TopicListView view, Optional<String> search) throws ExecutionException, InterruptedException {
        List<String> topics = all(clusterId, view, search)
            .stream()
            .collect(Collectors.toList());

        // XXX: The interface wants us to wrap these, so do that.
        return this.findByName(clusterId, topics)
            .stream()
            .map(CompletableFuture::completedFuture)
            .collect(Collectors.toList());
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

        Optional<Topic> topics = Optional.empty();
        if(isTopicMatchRegex(getTopicFilterRegex(),name)) {
            topics = this.findByName(clusterId, Collections.singletonList(name)).stream().findFirst();
        }
        return topics.orElseThrow(() -> new NoSuchElementException("Topic '" + name + "' doesn't exist"));
    }

    public List<Topic> findByName(String clusterId, List<String> topics) throws ExecutionException, InterruptedException {
        ArrayList<Topic> list = new ArrayList<>();

        Set<Map.Entry<String, TopicDescription>> topicDescriptions = kafkaWrapper.describeTopics(clusterId, topics).entrySet();
        Map<String, List<Partition.Offsets>> topicOffsets = kafkaWrapper.describeTopicsOffsets(clusterId, topics);

        Map<String, List<ConsumerGroup>> topicConsumerGroups = consumerGroupRepository.findByTopic(clusterId, topics);

        Optional<String> topicRegex = getTopicFilterRegex();

        for (Map.Entry<String, TopicDescription> description : topicDescriptions) {
            if(isTopicMatchRegex(topicRegex, description.getValue().name())){
                list.add(
                    new Topic(
                        description.getValue(),
                        topicConsumerGroups.getOrDefault(description.getValue().name(), Collections.emptyList()),
                        logDirRepository.findByTopic(clusterId, description.getValue().name()),
                        topicOffsets.get(description.getValue().name()),
                        isInternal(description.getValue().name()),
                        isStream(description.getValue().name())
                    )
                );
            }
        }

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

    public void create(String clusterId, String name, int partitions, short replicationFactor, List<org.kafkahq.models.Config> configs) throws ExecutionException, InterruptedException {
        kafkaModule
            .getAdminClient(clusterId)
            .createTopics(Collections.singleton(new NewTopic(name, partitions, replicationFactor)))
            .all()
            .get();

        configRepository.updateTopic(clusterId, name, configs);
    }

    public void delete(String clusterId, String name) throws ExecutionException, InterruptedException {
        kafkaModule.getAdminClient(clusterId)
            .deleteTopics(Collections.singleton(name))
            .all()
            .get();
    }

    private Optional<String> getTopicFilterRegex() {
        if (applicationContext.containsBean(SecurityService.class)) {
            SecurityService securityService = applicationContext.getBean(SecurityService.class);
            Optional<Authentication> authentication = securityService.getAuthentication();
            if (authentication.isPresent()) {
                Authentication auth = authentication.get();
                if (auth.getAttributes().get("topics-filter-regexp") != null) {
                    return Optional.of(auth.getAttributes().get("topics-filter-regexp").toString());
                }
            }
        }
        return Optional.empty();
    }

}
