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
import org.kafkahq.modules.KafkaWrapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class ConsumerGroupRepository extends AbstractRepository {
    @Inject
    KafkaWrapper kafkaWrapper;

    @Inject
    private KafkaModule kafkaModule;

    public List<CompletableFuture<ConsumerGroup>> list(String clusterId, Optional<String> search) throws ExecutionException, InterruptedException {
        ArrayList<String> list = new ArrayList<>();

        for (ConsumerGroupListing item : kafkaWrapper.listConsumerGroups(clusterId)) {
            if (isSearchMatch(search, item.groupId())) {
                list.add(item.groupId());
            }
        }

        list.sort(Comparator.comparing(String::toLowerCase));

        // XXX: The interface wants us to wrap these, so do that.
        return this.findByName(clusterId, list)
            .stream()
            .map(CompletableFuture::completedFuture)
            .collect(Collectors.toList());
    }

    public ConsumerGroup findByName(String clusterId, String name) throws ExecutionException, InterruptedException {
        Optional<ConsumerGroup> topics = this.findByName(clusterId, Collections.singletonList(name)).stream().findFirst();

        return topics.orElseThrow(() -> new NoSuchElementException("Topic '" + name + "' doesn't exist"));
    }

    public List<ConsumerGroup> findByName(String clusterId, List<String> groups) throws ExecutionException, InterruptedException {
        Map<String, ConsumerGroupDescription> consumerDescriptions = kafkaWrapper.describeConsumerGroups(clusterId, groups);
        Map<String, Map<TopicPartition, OffsetAndMetadata>> groupGroupsOffsets = consumerDescriptions.keySet().stream()
            .map(group -> {
                try {
                    return new AbstractMap.SimpleEntry<>(group, kafkaWrapper.consumerGroupsOffsets(clusterId, group));
                } catch (ExecutionException | InterruptedException e) {
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
            .collect(Collectors.toList());
    }

    public List<ConsumerGroup> findByTopic(String clusterId, String topic) throws ExecutionException, InterruptedException {
        Optional<List<ConsumerGroup>> consumerGroups = Optional.of(this.findByTopic(clusterId, Collections.singletonList(topic)).get(topic));

        return consumerGroups.orElse(Collections.emptyList());
    }

    public Map<String, List<ConsumerGroup>> findByTopic(String clusterId, List<String> topics) throws ExecutionException, InterruptedException {
        List<CompletableFuture<ConsumerGroup>> list = this.list(clusterId, Optional.empty());

        List<ConsumerGroup> completed = CompletableFuture.allOf(list.toArray(new CompletableFuture[0]))
            .thenApply(s ->
                list.stream().
                    map(CompletableFuture::join).
                    collect(Collectors.toList())
            )
            .get();

        return completed
            .stream()
            .flatMap(consumerGroup -> Stream.concat(consumerGroup.getActiveTopics().stream(), consumerGroup.getTopics().stream()).distinct().map(topic -> new AbstractMap.SimpleImmutableEntry<>(topic, consumerGroup)))
            .filter(entry -> topics.contains(entry.getKey()))
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
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
        consumer.close();
    }

    public void delete(String clusterId, String name) throws ExecutionException, InterruptedException {
        kafkaModule.getAdminClient(clusterId).deleteConsumerGroups(Collections.singleton(name)).all().get();
    }
}
