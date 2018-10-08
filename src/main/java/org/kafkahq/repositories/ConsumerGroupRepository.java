package org.kafkahq.repositories;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.jooby.Env;
import org.jooby.Jooby;
import org.kafkahq.models.ConsumerGroup;
import org.kafkahq.models.Partition;
import org.kafkahq.modules.KafkaModule;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class ConsumerGroupRepository extends AbstractRepository implements Jooby.Module {
    @Inject
    private KafkaModule kafkaModule;

    public List<ConsumerGroup> list() throws ExecutionException, InterruptedException {
        ArrayList<String> list = new ArrayList<>();

        for (ConsumerGroupListing item : kafkaWrapper.listConsumerGroups()) {
            list.add(item.groupId());
        }

        List<ConsumerGroup> groups = this.findByName(list);
        groups.sort(Comparator.comparing(ConsumerGroup::getId));

        return groups;
    }

    public ConsumerGroup findByName(String name) throws ExecutionException, InterruptedException {
        Optional<ConsumerGroup> topics = this.findByName(new ArrayList<String>() {{
            add(name);
        }}).stream().findFirst();

        return topics.orElseThrow(() -> new NoSuchElementException("Topic '" + name + "' doesn't exist"));
    }

    public List<ConsumerGroup> findByName(List<String> groups) throws ExecutionException, InterruptedException {
        ArrayList<ConsumerGroup> list = new ArrayList<>();

        groups = groups.stream().map(s -> s.equals("null") ? "" : s).collect(Collectors.toList());

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
        return this.list().stream()
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

    public void delete(String clusterId, String name) throws ExecutionException, InterruptedException {
        kafkaModule.getAdminClient(clusterId).deleteConsumerGroups(new ArrayList<String>() {{
            add(name);
        }}).all().get();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(ConsumerGroupRepository.class).toInstance(new ConsumerGroupRepository());
    }
}
