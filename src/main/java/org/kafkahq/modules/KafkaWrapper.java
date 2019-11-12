package org.kafkahq.modules;


import io.micronaut.cache.annotation.Cacheable;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.ClusterAuthorizationException;
import org.apache.kafka.common.errors.SecurityDisabledException;
import org.apache.kafka.common.requests.DescribeLogDirsResponse;
import org.kafkahq.models.Partition;
import org.kafkahq.utils.Lock;
import org.kafkahq.utils.MethodCacheKeyGenerator;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Cacheable(cacheNames = "kafka-wrapper", keyGenerator = MethodCacheKeyGenerator.class)
public class KafkaWrapper {
    @Inject
    private KafkaModule kafkaModule;

    public DescribeClusterResult describeCluster(String clusterId) throws ExecutionException, InterruptedException {
        return Lock.call(() -> {
            DescribeClusterResult cluster = kafkaModule.getAdminClient(clusterId).describeCluster();

            cluster.clusterId().get();
            cluster.nodes().get();
            cluster.controller().get();

            return cluster;
        }, "Cluster", null);
    }


    public Collection<TopicListing> listTopics(String clusterId) throws ExecutionException, InterruptedException {
        return Lock.call(
            () -> kafkaModule.getAdminClient(clusterId).listTopics(
                new ListTopicsOptions().listInternal(true)
            ).listings().get(),
            "List topics",
            null
        );
    }

    public Map<String, TopicDescription> describeTopics(String clusterId, List<String> topics) throws ExecutionException, InterruptedException {
        return Lock.call(
            () -> kafkaModule.getAdminClient(clusterId)
                .describeTopics(topics)
                .all()
                .get(),
            "Describe Topics {}",
            topics
        );
    }

    public Map<String, List<Partition.Offsets>> describeTopicsOffsets(String clusterId, List<String> topics) throws ExecutionException, InterruptedException {
        return Lock.call(
            () -> {
                List<TopicPartition> collect = this.describeTopics(clusterId, topics).entrySet()
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

                KafkaConsumer<byte[], byte[]> consumer = kafkaModule.getConsumer(clusterId);
                Map<TopicPartition, Long> begins = consumer.beginningOffsets(collect);
                Map<TopicPartition, Long> ends = consumer.endOffsets(collect);
                consumer.close();

                return begins.entrySet().stream()
                    .collect(groupingBy(
                        o -> o.getKey().topic(),
                        mapping(
                            begin ->
                                new Partition.Offsets(
                                    begin.getKey().partition(),
                                    begin.getValue(),
                                    ends.get(begin.getKey())
                                ),
                            toList()
                        )
                    ));
            },
            "Describe Topics Offsets {}",
            topics
        );
    }


    public Collection<ConsumerGroupListing> listConsumerGroups(String clusterId) throws ExecutionException, InterruptedException {
        return Lock.call(
            () -> kafkaModule.getAdminClient(clusterId).listConsumerGroups().all().get(),
            "List ConsumerGroups",
            null
        );
    }

    public Map<String, ConsumerGroupDescription> describeConsumerGroups(String clusterId, List<String> topics) throws ExecutionException, InterruptedException {
        return Lock.call(
            () -> kafkaModule.getAdminClient(clusterId)
                .describeConsumerGroups(topics)
                .all()
                .get(),
            "Describe ConsumerGroups {}",
            topics
        );
    }

    public Map<TopicPartition, OffsetAndMetadata> consumerGroupsOffsets(String clusterId, String groupId) throws ExecutionException, InterruptedException {
        return Lock.call(
            () -> kafkaModule.getAdminClient(clusterId)
                .listConsumerGroupOffsets(groupId)
                .partitionsToOffsetAndMetadata()
                .get(),
            "ConsumerGroup Offsets {}",
            Collections.singletonList(groupId)
        );
    }

    public Map<Integer, Map<String, DescribeLogDirsResponse.LogDirInfo>> describeLogDir(String clusterId) throws ExecutionException, InterruptedException {
        return Lock.call(
            () -> {
                try {
                    return kafkaModule.getAdminClient(clusterId)
                        .describeLogDirs(this.describeCluster(clusterId).nodes().get()
                            .stream()
                            .map(Node::id)
                            .collect(Collectors.toList())
                        )
                        .all()
                        .get();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof ClusterAuthorizationException) {
                        return new HashMap<>();
                    }

                    throw e;
                }
            },
            "List Log dir",
            null
        );
    }

    public Map<ConfigResource, Config> describeConfigs(String clusterId, ConfigResource.Type type, List<String> names) throws ExecutionException, InterruptedException {
        return Lock.call(() -> kafkaModule.getAdminClient(clusterId)
            .describeConfigs(names.stream()
                .map(s -> new ConfigResource(type, s))
                .collect(Collectors.toList())
            )
            .all()
            .get(),
            "Describe Topic Config {}",
            names
        );
    }

    public Collection<AclBinding> describeAcls(String clusterId, AclBindingFilter filter) throws ExecutionException, InterruptedException {
        return Lock.call(() -> {
                    try {
                        return kafkaModule.getAdminClient(clusterId)
                                .describeAcls(filter)
                                .values()
                                .get();
                    } catch (ExecutionException e) {
                        if (e.getCause() instanceof SecurityDisabledException || e.getCause() instanceof ClusterAuthorizationException ) {
                            return Collections.emptyList();
                        }
                        throw e;
                    }
                },
                "Describe Acls config",
                null
        );
    }
}
