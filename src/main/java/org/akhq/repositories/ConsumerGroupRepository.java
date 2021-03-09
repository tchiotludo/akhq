package org.akhq.repositories;

import io.micronaut.context.ApplicationContext;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.utils.SecurityService;
import org.akhq.configs.SecurityProperties;
import org.akhq.models.ConsumerGroup;
import org.akhq.models.Partition;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.modules.KafkaModule;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;
import org.akhq.utils.UserGroupUtils;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class ConsumerGroupRepository extends AbstractRepository {
    @Inject
    AbstractKafkaWrapper kafkaWrapper;

    @Inject
    private KafkaModule kafkaModule;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private UserGroupUtils userGroupUtils;

    @Inject
    private SecurityProperties securityProperties;

    public PagedList<ConsumerGroup> list(String clusterId, Pagination pagination, Optional<String> search) throws ExecutionException, InterruptedException {
        return PagedList.of(all(clusterId, search), pagination, groupsList -> this.findByName(clusterId, groupsList));
    }

    public List<String> all(String clusterId, Optional<String> search) throws ExecutionException, InterruptedException {
        ArrayList<String> list = new ArrayList<>();

        for (ConsumerGroupListing item : kafkaWrapper.listConsumerGroups(clusterId)) {
            if (isSearchMatch(search, item.groupId()) && isMatchRegex(
                    getConsumerGroupFilterRegex(), item.groupId())) {
                list.add(item.groupId());
            }
        }

        list.sort(Comparator.comparing(String::toLowerCase));

        return list;
    }

    public ConsumerGroup findByName(String clusterId, String name) throws ExecutionException, InterruptedException {
        Optional<ConsumerGroup> consumerGroup = Optional.empty();
        if(isMatchRegex(getConsumerGroupFilterRegex(),name)) {
           consumerGroup = this.findByName(clusterId, Collections.singletonList(name)).stream().findFirst();
        }
        return consumerGroup.orElseThrow(() -> new NoSuchElementException("Consumer Group '" + name + "' doesn't exist"));
    }

    public List<ConsumerGroup> findByName(String clusterId, List<String> groups) throws ExecutionException, InterruptedException {
        Optional<List<String>> consumerGroupRegex = getConsumerGroupFilterRegex();
        List<String> filteredConsumerGroups = groups.stream()
                .filter(t -> isMatchRegex(consumerGroupRegex, t))
                .collect(Collectors.toList());
        Map<String, ConsumerGroupDescription> consumerDescriptions = kafkaWrapper.describeConsumerGroups(clusterId, filteredConsumerGroups);

        Map<String, Map<TopicPartition, OffsetAndMetadata>> groupGroupsOffsets = consumerDescriptions.keySet().stream()
            .map(group -> {
                try {
                    return new AbstractMap.SimpleEntry<>(group, kafkaWrapper.consumerGroupsOffsets(clusterId, group));
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<String> topics = groupGroupsOffsets.values().stream()
            .map(Map::keySet)
            .flatMap(Set::stream)
            .map(TopicPartition::topic)
            .distinct()
            .collect(Collectors.toList());
        Map<String, List<Partition.Offsets>> topicTopicsOffsets = kafkaWrapper.describeTopicsOffsets(clusterId, topics);

        return consumerDescriptions.values().stream()
            .map(consumerGroupDescription -> new ConsumerGroup(
                consumerGroupDescription,
                groupGroupsOffsets.get(consumerGroupDescription.groupId()),
                groupGroupsOffsets.get(consumerGroupDescription.groupId()).keySet().stream()
                    .map(TopicPartition::topic)
                    .distinct()
                    .collect(Collectors.toMap(Function.identity(), topicTopicsOffsets::get))
            ))
            .sorted(Comparator.comparing(ConsumerGroup::getId))
            .collect(Collectors.toList());
    }

    public List<ConsumerGroup> findByTopic(String clusterId, String topic) throws ExecutionException, InterruptedException {
        List<String> groupName = this.all(clusterId, Optional.empty());
        List<ConsumerGroup> list = this.findByName(clusterId, groupName);

        return list
            .stream()
            .filter(consumerGroups ->
                consumerGroups.getActiveTopics()
                    .stream()
                    .anyMatch(s -> Objects.equals(s, topic)) ||
                    consumerGroups.getTopics()
                        .stream()
                        .anyMatch(s -> Objects.equals(s, topic))
            )
            .collect(Collectors.toList());
    }

    public List<ConsumerGroup> findByTopics(String clusterId, List<String> topics) throws ExecutionException, InterruptedException {

        List<String> groupName = this.all(clusterId, Optional.empty());
        List<ConsumerGroup> list = this.findByName(clusterId, groupName);
        return list
                .stream()
                .filter(consumerGroups ->
                        consumerGroups.getActiveTopics()
                                .stream()
                                .anyMatch(s -> topics.contains(s) ) ||
                                consumerGroups.getTopics()
                                        .stream()
                                        .anyMatch(s -> topics.contains(s))
                )
                .collect(Collectors.toList());
    }

    public void updateOffsets(String clusterId, String name, Map<org.akhq.models.TopicPartition, Long> offset) {
        KafkaConsumer<byte[], byte[]> consumer = kafkaModule.getConsumer(clusterId, new Properties() {{
            put(ConsumerConfig.GROUP_ID_CONFIG, name);
        }});

        Map<TopicPartition, OffsetAndMetadata> offsets = offset
            .entrySet()
            .stream()
            .map(r -> new AbstractMap.SimpleEntry<>(
                new TopicPartition(r.getKey().getTopic(), r.getKey().getPartition()),
                new OffsetAndMetadata(r.getValue())
            ))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        consumer.commitSync(offsets);
        consumer.close();

        kafkaWrapper.clearConsumerGroupsOffsets();
    }

    private Optional<List<String>> getConsumerGroupFilterRegex() {

        List<String> consumerGroupFilterRegex = new ArrayList<>();

        if (applicationContext.containsBean(SecurityService.class)) {
            SecurityService securityService = applicationContext.getBean(SecurityService.class);
            Optional<Authentication> authentication = securityService.getAuthentication();
            if (authentication.isPresent()) {
                Authentication auth = authentication.get();
                consumerGroupFilterRegex.addAll(getConsumerGroupFilterRegexFromAttributes(auth.getAttributes()));
            }
        }
        // get consumer group filter regex for default groups
        consumerGroupFilterRegex.addAll(getConsumerGroupFilterRegexFromAttributes(
                userGroupUtils.getUserAttributes(Collections.singletonList(securityProperties.getDefaultGroup()))
        ));

        return Optional.of(consumerGroupFilterRegex);
    }

    @SuppressWarnings("unchecked")
    private List<String> getConsumerGroupFilterRegexFromAttributes(Map<String, Object> attributes) {
        if (attributes.get("consumerGroupsFilterRegexp") != null) {
            if (attributes.get("consumerGroupsFilterRegexp") instanceof List) {
                return (List<String>)attributes.get("consumerGroupsFilterRegexp");
            }
        }
        return new ArrayList<>();
    }
}
