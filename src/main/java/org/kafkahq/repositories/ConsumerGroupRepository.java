package org.kafkahq.repositories;

import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.kafkahq.models.ConsumerGroup;
import org.kafkahq.models.Partition;
import org.kafkahq.modules.KafkaModule;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class ConsumerGroupRepository extends AbstractRepository {
    private KafkaModule kafkaModule;

    @Inject
    public ConsumerGroupRepository(KafkaModule kafkaModule) {
        this.kafkaModule = kafkaModule;
    }

    public List<ConsumerGroup> list(Optional<String> search) throws ExecutionException, InterruptedException {
        ArrayList<String> list = new ArrayList<>();

        for (ConsumerGroupListing item : kafkaWrapper.listConsumerGroups()) {
            if (isSearchMatch(search, item.groupId())) {
                list.add(item.groupId());
            }
        }

        List<ConsumerGroup> groups = this.findByName(list);
        groups.sort(Comparator.comparing(ConsumerGroup::getId));

        return groups;
    }

    public ConsumerGroup findByName(String name) throws ExecutionException, InterruptedException {
        Optional<ConsumerGroup> topics = this.findByName(Collections.singletonList(name)).stream().findFirst();

        return topics.orElseThrow(() -> new NoSuchElementException("Topic '" + name + "' doesn't exist"));
    }

    public List<ConsumerGroup> findByName(List<String> groups) throws ExecutionException, InterruptedException {
        ArrayList<ConsumerGroup> list = new ArrayList<>();

        Set<Map.Entry<String, ConsumerGroupDescription>> consumerDescriptions = kafkaWrapper.describeConsumerGroups(groups).entrySet();

        for (Map.Entry<String, ConsumerGroupDescription> description : consumerDescriptions) {
            Map<TopicPartition, OffsetAndMetadata> groupsOffsets = kafkaWrapper.consumerGroupsOffsets(description.getKey());
            Map<String, List<Partition.Offsets>> topicsOffsets = kafkaWrapper.describeTopicsOffsets(groupsOffsets.entrySet()
                .stream()
                .map(item -> item.getKey().topic())
                .distinct()
                .collect(Collectors.toList())
            );

            list.add(new ConsumerGroup(
                description.getValue(),
                groupsOffsets,
                topicsOffsets
            ));
        }

        return list;
    }

    public List<ConsumerGroup> findByTopic(String topic) throws ExecutionException, InterruptedException {
        return this.list(Optional.empty()).stream()
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

    public void updateOffsets(String clusterId, String name, Map<org.kafkahq.models.TopicPartition, Long> offset) {
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
    }

    public void delete(String clusterId, String name) throws ExecutionException, InterruptedException {
        kafkaModule.getAdminClient(clusterId).deleteConsumerGroups(Collections.singleton(name)).all().get();
    }
}
