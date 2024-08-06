package org.akhq.repositories;

import io.micronaut.context.ApplicationContext;
import org.akhq.models.ConsumerGroup;
import org.akhq.models.Partition;
import org.akhq.models.audit.ConsumerGroupAuditEvent;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.modules.AuditModule;
import org.akhq.modules.KafkaModule;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
    private AuditModule auditModule;

    public PagedList<ConsumerGroup> list(String clusterId, Pagination pagination, Optional<String> search, List<String> filters) throws ExecutionException, InterruptedException {
        return PagedList.of(all(clusterId, search, filters), pagination, groupsList -> this.findByName(clusterId, groupsList, filters));
    }

    public List<String> all(String clusterId, Optional<String> search, List<String> filters) {
        try {
            return kafkaWrapper.listConsumerGroups(clusterId).stream()
                .map(ConsumerGroupListing::groupId)
                .filter(groupId -> isSearchMatch(search, groupId) && isMatchRegex(filters, groupId))
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public ConsumerGroup findByName(String clusterId, String name, List<String> filters) throws ExecutionException, InterruptedException {
        Optional<ConsumerGroup> consumerGroup = Optional.empty();
        if (isMatchRegex(filters, name)) {
            consumerGroup = this.findByName(clusterId, Collections.singletonList(name), filters).stream().findFirst();
        }
        return consumerGroup.orElseThrow(() -> new NoSuchElementException("Consumer Group '" + name + "' doesn't exist"));
    }

    /**
     * Find all the consumer groups matching the list of names
     * Execution time can be long if the list of groups is big. This method does several calls to the Kafka cluster
     * (describeConsumerGroups, listConsumerGroupOffsets, describeTopics) to build ConsumerGroup with:
     * - consumer groups description containing the active members
     * - consumer groups offsets (allowing to retrieve the topics for empty consumer groups)
     * - topics partition offsets (allowing to compute the lag)
     *
     * @param clusterId cluster id
     * @param groups list of consumer group names
     * @param filters security filters applied to the user
     * @return list of consumer groups
     */
    public List<ConsumerGroup> findByName(String clusterId, List<String> groups, List<String> filters) {
        List<String> filteredConsumerGroups = groups.stream()
            .filter(t -> isMatchRegex(filters, t))
            .collect(Collectors.toList());

        try {
            // Get the consumer group description of all the consumer groups the user has access to
            Map<String, ConsumerGroupDescription> consumerDescriptions = kafkaWrapper.describeConsumerGroups(clusterId, filteredConsumerGroups);

            // Get the consumer group offsets to get also the topics on which the groups are affected
            Map<String, Map<TopicPartition, OffsetAndMetadata>> groupGroupsOffsets = consumerDescriptions.keySet().stream()
                .map(group -> {
                    try {
                        return new AbstractMap.SimpleEntry<>(group, kafkaWrapper.consumerGroupsOffsets(clusterId, group));
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // Extract the topics list
            List<String> topics = groupGroupsOffsets.values().stream()
                .map(Map::keySet)
                .flatMap(Set::stream)
                .map(TopicPartition::topic)
                .distinct()
                .collect(Collectors.toList());

            // Get the topics offsets to be able to extract later the offsets of empty consumer groups
            Map<String, List<Partition.Offsets>> topicTopicsOffsets = kafkaWrapper.describeTopicsOffsets(clusterId, topics);

            // Build the consumer groups with all the information collected before
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
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Find all the active consumer groups for a list of topics
     * Use the ConsumerGroupDescription to find the active topics from the consumer group description members.
     *
     * @param clusterId cluster id
     * @param topics topics name
     * @param filters security filters applied to the user
     * @return list of consumer groups
     */
    public List<ConsumerGroup> findActiveByTopics(String clusterId, List<String> topics, List<String> filters) {
        List<String> groupsName = this.all(clusterId, Optional.empty(), filters);

        // Keep only the consumer groups that have the topic(s) in their active topics
        List<String> consumerGroups =
            kafkaWrapper.describeConsumerGroups(clusterId, groupsName)
                .values().stream()
                .filter(description ->
                    description.members()
                        .stream()
                        .flatMap(member -> member.assignment().topicPartitions()
                            .stream().map(TopicPartition::topic))
                        .distinct()
                        .anyMatch(topics::contains)
                )
                .map(ConsumerGroupDescription::groupId)
                .toList();

        return this.findByName(clusterId, consumerGroups, filters);
    }

    public List<ConsumerGroup> findActiveByTopic(String clusterId, String topic, List<String> filters) {
        return findActiveByTopics(clusterId, List.of(topic), filters);
    }

    /**
     * Find all the consumer groups for a topic, empty or not.
     * Warning: this method loops over all the consumer groups in the cluster because it searches for the empty consumer groups too.
     * Consider using findActiveByTopic if searching for the active consumer groups is enough because. The execution time will be faster
     *
     * @param clusterId cluster id
     * @param topic topic name
     * @param filters security filters applied to the user
     * @return list of consumer groups
     */
    public List<ConsumerGroup> findByTopic(String clusterId, String topic, List<String> filters) throws ExecutionException, InterruptedException {
        List<String> groupName = this.all(clusterId, Optional.empty(), filters);
        List<ConsumerGroup> list = this.findByName(clusterId, groupName, filters);

        return list.stream()
            .filter(consumerGroup -> consumerGroup.getTopics().stream()
                .anyMatch(s -> Objects.equals(s, topic)))
            .collect(Collectors.toList());
    }

    public List<ConsumerGroup> findByTopics(String clusterId, List<String> topics, List<String> filters) {

        List<String> groupName = this.all(clusterId, Optional.empty(), filters);
        List<ConsumerGroup> list = this.findByName(clusterId, groupName, filters);
        return list
            .stream()
            .filter(consumerGroups ->
                    consumerGroups.getTopics()
                        .stream()
                        .anyMatch(topics::contains)
            )
            .toList();
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

        offset.forEach(
            (k, v) -> auditModule.save(ConsumerGroupAuditEvent.updateOffsets(clusterId, k.getTopic(), name))
        );


        kafkaWrapper.clearConsumerGroupsOffsets();
    }
}
