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
import java.util.stream.Collectors;

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

        return list
            .stream()
            .map(s -> CompletableFuture.supplyAsync(() -> {
                try {
                    return this.findByName(clusterId, s);
                }
                catch(ExecutionException | InterruptedException ex) {
                    throw new CompletionException(ex);
                }
            }))
            .collect(Collectors.toList());
    }

    public ConsumerGroup findByName(String clusterId, String name) throws ExecutionException, InterruptedException {
        Optional<ConsumerGroup> topics = this.findByName(clusterId, Collections.singletonList(name)).stream().findFirst();

        return topics.orElseThrow(() -> new NoSuchElementException("Topic '" + name + "' doesn't exist"));
    }

    public List<ConsumerGroup> findByName(String clusterId, List<String> groups) throws ExecutionException, InterruptedException {
        ArrayList<ConsumerGroup> list = new ArrayList<>();

        Set<Map.Entry<String, ConsumerGroupDescription>> consumerDescriptions = kafkaWrapper.describeConsumerGroups(clusterId, groups).entrySet();

        for (Map.Entry<String, ConsumerGroupDescription> description : consumerDescriptions) {
            Map<TopicPartition, OffsetAndMetadata> groupsOffsets = kafkaWrapper.consumerGroupsOffsets(clusterId, description.getKey());
            Map<String, List<Partition.Offsets>> topicsOffsets = kafkaWrapper.describeTopicsOffsets(clusterId, groupsOffsets.entrySet()
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

    public List<ConsumerGroup> findByTopic(String clusterId, String topic) throws ExecutionException, InterruptedException {
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
        consumer.close();
    }

    public void delete(String clusterId, String name) throws ExecutionException, InterruptedException {
        kafkaModule.getAdminClient(clusterId).deleteConsumerGroups(Collections.singleton(name)).all().get();
    }
}
