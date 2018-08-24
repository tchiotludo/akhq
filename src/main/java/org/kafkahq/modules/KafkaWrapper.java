package org.kafkahq.modules;

import com.google.inject.Inject;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.requests.DescribeLogDirsResponse;
import org.jooby.scope.RequestScoped;
import org.kafkahq.models.Partition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@RequestScoped
public class KafkaWrapper {
    private static Logger logger = LoggerFactory.getLogger(KafkaWrapper.class);

    private KafkaModule kafkaModule;

    private String clusterId;
    
    @Inject
    public KafkaWrapper(KafkaModule kafkaModule, String clusterId) {
        this.kafkaModule = kafkaModule;
        this.clusterId = clusterId;
    }

    private DescribeClusterResult cluster;

    public DescribeClusterResult describeCluster() throws ExecutionException, InterruptedException {
        if (this.cluster == null) {
            this.cluster = this.kafkaModule.debug(() -> {
                DescribeClusterResult cluster = kafkaModule.getAdminClient(clusterId).describeCluster();

                cluster.clusterId().get();
                cluster.nodes().get();
                cluster.controller().get();
                return cluster;
            }, "Cluster");
        }

        return this.cluster;
    }

    private Collection<TopicListing> listTopics;

    public Collection<TopicListing> listTopics() throws ExecutionException, InterruptedException {
        if (this.listTopics == null) {
            this.listTopics = this.kafkaModule.debug(
                () -> kafkaModule.getAdminClient(clusterId).listTopics(
                    new ListTopicsOptions().listInternal(true)
                ).listings().get(),
                "List topics"
            );
        }

        return this.listTopics;
    }

    private Map<String, TopicDescription> describeTopics = new HashMap<>();

    public Map<String, TopicDescription> describeTopics(List<String> topics) throws ExecutionException, InterruptedException {
        List<String> list = new ArrayList<>(topics);
        list.removeIf(value -> this.describeTopics.containsKey(value));

        if (list.size() > 0) {
            Map<String, TopicDescription> description = this.kafkaModule.debug(
                () -> kafkaModule.getAdminClient(clusterId)
                    .describeTopics(list)
                    .all()
                    .get(),
                "Describe Topic {}",
                topics
            );

            this.describeTopics.putAll(description);
        }

        return this.describeTopics
            .entrySet()
            .stream()
            .filter(e -> topics.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, List<Partition.Offsets>> describeTopicsOffsets = new HashMap<>();

    public Map<String, List<Partition.Offsets>> describeTopicsOffsets(List<String> topics) throws ExecutionException, InterruptedException {
        List<String> list = new ArrayList<>(topics);
        list.removeIf(value -> this.describeTopicsOffsets.containsKey(value));

        if (list.size() > 0) {
            Map<String, List<Partition.Offsets>> finalOffsets = this.kafkaModule.debug(
                () -> {
                    List<TopicPartition> collect = this.describeTopics(topics).entrySet()
                        .stream()
                        .flatMap(topicDescription -> topicDescription
                            .getValue()
                            .partitions()
                            .stream()
                            .map(topicPartitionInfo ->
                                new TopicPartition(topicDescription.getValue().name(), topicPartitionInfo.partition())
                            )
                        )
                        .collect(Collectors.toList());

                    Map<TopicPartition, Long> begins = kafkaModule.getConsumer(clusterId).beginningOffsets(collect);
                    // @FIXME: ugly hacks, on startup, first query can send a partial result, resending request works !
                    if (begins.size() != collect.size()) {
                        begins = kafkaModule.getConsumer(clusterId).beginningOffsets(collect);
                    }
                    Map<TopicPartition, Long> ends = kafkaModule.getConsumer(clusterId).endOffsets(collect);

                    return begins.entrySet().stream()
                        .collect(groupingBy(
                            o -> o.getKey().topic(),
                            mapping(begin ->
                                    new Partition.Offsets(
                                        begin.getKey().partition(),
                                        begin.getValue(),
                                        ends.get(begin.getKey())
                                    ),
                                toList()
                            )
                        ));
                },
                "Describe Topic {}",
                topics
            );

            this.describeTopicsOffsets.putAll(finalOffsets);
        }

        return this.describeTopicsOffsets
            .entrySet()
            .stream()
            .filter(e -> topics.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Collection<ConsumerGroupListing> listConsumerGroups;

    public Collection<ConsumerGroupListing> listConsumerGroups() throws ExecutionException, InterruptedException {
        if (this.listConsumerGroups == null) {
            this.listConsumerGroups = this.kafkaModule.debug(
                () -> kafkaModule.getAdminClient(clusterId).listConsumerGroups().all().get(),
                "List ConsumerGroups"
            );
        }

        return this.listConsumerGroups;
    }

    private Map<String, ConsumerGroupDescription> describeConsumerGroups = new HashMap<>();

    public Map<String, ConsumerGroupDescription> describeConsumerGroups(List<String> topics) throws ExecutionException, InterruptedException {
        List<String> list = new ArrayList<>(topics);
        list.removeIf(value -> this.describeConsumerGroups.containsKey(value));

        if (list.size() > 0) {
            Map<String, ConsumerGroupDescription> description = this.kafkaModule.debug(
                () -> kafkaModule.getAdminClient(clusterId)
                    .describeConsumerGroups(list)
                    .all()
                    .get(),
                "Describe ConsumerGroups {}",
                topics
            );

            this.describeConsumerGroups.putAll(description);
        }

        return this.describeConsumerGroups
            .entrySet()
            .stream()
            .filter(e -> topics.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    private Map<String, Map<TopicPartition, OffsetAndMetadata>> consumerGroupOffset = new HashMap<>();

    public Map<TopicPartition, OffsetAndMetadata> consumerGroupsOffsets(String groupId) throws ExecutionException, InterruptedException {
        if (!this.consumerGroupOffset.containsKey(groupId)) {
            Map<TopicPartition, OffsetAndMetadata> description = this.kafkaModule.debug(
                () -> kafkaModule.getAdminClient(clusterId)
                    .listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata()
                    .get(),
                "ConsumerGroup Offsets {}",
                groupId
            );

            this.consumerGroupOffset.put(groupId, description);
        }

        return this.consumerGroupOffset.get(groupId);
    }

    private Map<Integer, Map<String, DescribeLogDirsResponse.LogDirInfo>> logDirs;

    public Map<Integer, Map<String, DescribeLogDirsResponse.LogDirInfo>> describeLogDir() throws ExecutionException, InterruptedException {
        if (this.logDirs == null) {
            this.logDirs = this.kafkaModule.debug(() ->
                    kafkaModule.getAdminClient(clusterId)
                        .describeLogDirs(this.describeCluster().nodes().get()
                            .stream()
                            .map(Node::id)
                            .collect(Collectors.toList())
                        )
                        .all()
                        .get(),
                "List Log dir"
            );
        }

        return this.logDirs;
    }
}
