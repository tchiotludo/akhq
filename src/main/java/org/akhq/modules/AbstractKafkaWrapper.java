package org.akhq.modules;


import com.google.common.collect.ImmutableMap;
import org.akhq.models.Partition;
import org.akhq.utils.Logger;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.ApiException;
import org.apache.kafka.common.errors.ClusterAuthorizationException;
import org.apache.kafka.common.errors.SecurityDisabledException;
import org.apache.kafka.common.errors.TopicAuthorizationException;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.inject.Inject;

import static java.util.stream.Collectors.*;

abstract public class AbstractKafkaWrapper {
    @Inject
    private KafkaModule kafkaModule;

    private final Map<String, DescribeClusterResult> cluster = new HashMap<>();

    public DescribeClusterResult describeCluster(String clusterId) throws ExecutionException {
        if (!this.cluster.containsKey(clusterId)) {
            DescribeClusterResult cluster = kafkaModule.getAdminClient(clusterId).describeCluster();

            Logger.call(cluster.clusterId(), "Get cluster");
            Logger.call(cluster.nodes() , "Get nodes");
            Logger.call(cluster.controller() , "Get contoller");

            return cluster;
        }
        return this.cluster.get(clusterId);
    }

    private Map<String, Collection<TopicListing>> listTopics = new HashMap<>();

    public Collection<TopicListing> listTopics(String clusterId) throws ExecutionException {
        if (!this.listTopics.containsKey(clusterId)) {
            this.listTopics.put(clusterId, Logger.call(
                kafkaModule.getAdminClient(clusterId).listTopics(
                    new ListTopicsOptions().listInternal(true)
                ).listings(),
                "List topics"
            ));
        }

        return this.listTopics.get(clusterId);
    }

    private final Map<String, Map<String, TopicDescription>> describeTopics = new HashMap<>();

    public Map<String, TopicDescription> describeTopics(String clusterId, List<String> topics) throws ExecutionException {
        describeTopics.computeIfAbsent(clusterId, s -> new HashMap<>());

        List<String> list = new ArrayList<>(topics);
        list.removeIf(value -> this.describeTopics.get(clusterId).containsKey(value));

        if (list.size() > 0) {
            Map<String, TopicDescription> description = Logger.call(
                kafkaModule.getAdminClient(clusterId)
                    .describeTopics(list)
                    .all(),
                "Describe Topics {}",
                topics
            );

            this.describeTopics.get(clusterId).putAll(description);
        }

        return this.describeTopics
            .get(clusterId)
            .entrySet()
            .stream()
            .filter(e -> topics.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void createTopics(String clusterId, String name, int partitions, short replicationFactor) throws ExecutionException {
        Logger.call(kafkaModule
            .getAdminClient(clusterId)
            .createTopics(Collections.singleton(new NewTopic(name, partitions, replicationFactor)))
            .all(),
            "Create Topics",
            Collections.singletonList(name)
        );

        listTopics = new HashMap<>();
    }

    public void deleteTopics(String clusterId, String name) throws ExecutionException {
        Logger.call(kafkaModule.getAdminClient(clusterId)
            .deleteTopics(Collections.singleton(name))
            .all(),
            "Delete Topic",
            Collections.singletonList(name)
        );

        listTopics = new HashMap<>();
    }

    private final Map<String, Map<String, List<Partition.Offsets>>> describeTopicsOffsets = new HashMap<>();

    public Map<String, List<Partition.Offsets>> describeTopicsOffsets(String clusterId, List<String> topics) throws ExecutionException, InterruptedException {
        describeTopicsOffsets.computeIfAbsent(clusterId, s -> new HashMap<>());

        List<String> list = new ArrayList<>(topics);
        list.removeIf(value -> this.describeTopicsOffsets.get(clusterId).containsKey(value));

        if (list.size() > 0) {
            Map<String, List<Partition.Offsets>> finalOffsets = Logger.call(
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

            this.describeTopicsOffsets.get(clusterId).putAll(finalOffsets);
        }

        return this.describeTopicsOffsets.get(clusterId);
    }

    private final Map<String, Collection<ConsumerGroupListing>> listConsumerGroups = new HashMap<>();

    public Collection<ConsumerGroupListing> listConsumerGroups(String clusterId) throws ExecutionException {
        if (!this.listConsumerGroups.containsKey(clusterId)) {
            this.listConsumerGroups.put(clusterId, Logger.call(
                kafkaModule.getAdminClient(clusterId).listConsumerGroups().all(),
                "List ConsumerGroups",
                null
            ));
        }

        return this.listConsumerGroups.get(clusterId);
    }

    private Map<String, Map<String, ConsumerGroupDescription>> describeConsumerGroups = new HashMap<>();

    public Map<String, ConsumerGroupDescription> describeConsumerGroups(String clusterId, List<String> groups) throws ExecutionException {
        describeConsumerGroups.computeIfAbsent(clusterId, s -> new HashMap<>());

        List<String> list = new ArrayList<>(groups);
        list.removeIf(value -> this.describeConsumerGroups.get(clusterId).containsKey(value));

        if (list.size() > 0) {
            Map<String, ConsumerGroupDescription> description = Logger.call(
                kafkaModule.getAdminClient(clusterId)
                    .describeConsumerGroups(groups)
                    .all(),
                "Describe ConsumerGroups {}",
                groups
            );

            this.describeConsumerGroups.get(clusterId).putAll(description);
        }

        return this.describeConsumerGroups
            .get(clusterId)
            .entrySet()
            .stream()
            .filter(e -> groups.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void deleteConsumerGroups(String clusterId, String name) throws ApiException, ExecutionException {
        Logger.call(kafkaModule
            .getAdminClient(clusterId)
            .deleteConsumerGroups(Collections.singleton(name))
            .all(),
            "deleteConsumerGroups",
            Collections.singletonList(name)
        );

        describeConsumerGroups = new HashMap<>();
        consumerGroupOffset = new HashMap<>();
    }

    private Map<String, Map<String, Map<TopicPartition, OffsetAndMetadata>>> consumerGroupOffset = new HashMap<>();

    public Map<TopicPartition, OffsetAndMetadata> consumerGroupsOffsets(String clusterId, String groupId) throws ExecutionException {
        consumerGroupOffset.computeIfAbsent(clusterId, s -> new HashMap<>());

        if (!this.consumerGroupOffset.get(clusterId).containsKey(groupId)) {
            this.consumerGroupOffset.get(clusterId).put(groupId, Logger.call(
                kafkaModule.getAdminClient(clusterId)
                    .listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata(),
                "ConsumerGroup Offsets {}",
                Collections.singletonList(groupId)
            ));
        }

        return this.consumerGroupOffset.get(clusterId).get(groupId);
    }

    public void clearConsumerGroupsOffsets() {
        this.consumerGroupOffset = new HashMap<>();
    }

    private final Map<String, Map<Integer, Map<String, LogDirDescription>>> logDirs = new HashMap<>();

    public Map<Integer, Map<String, LogDirDescription>> describeLogDir(String clusterId) throws ExecutionException, InterruptedException {
        if (!this.logDirs.containsKey(clusterId)) {
            this.logDirs.put(clusterId, Logger.call(
                () -> {
                    try {
                        return kafkaModule.getAdminClient(clusterId)
                            .describeLogDirs(this.describeCluster(clusterId).nodes().get()
                                .stream()
                                .map(Node::id)
                                .collect(Collectors.toList())
                            )
                            .allDescriptions()
                            .get();
                    } catch (ExecutionException e) {
                        if (e.getCause() instanceof ClusterAuthorizationException || e.getCause() instanceof TopicAuthorizationException) {
                            return new HashMap<>();
                        }

                        if (e.getCause() instanceof ApiException) {
                            throw (ApiException) e.getCause();
                        }

                        throw e;
                    }
                },
                "List Log dir",
                null
            ));
        }

        return this.logDirs.get(clusterId);
    }

    private Map<String, Map<ConfigResource, Config>> describeConfigs = new HashMap<>();

    public Map<ConfigResource, Config> describeConfigs(String clusterId, ConfigResource.Type type, List<String> names) throws ExecutionException, InterruptedException {
        describeConfigs.computeIfAbsent(clusterId, s -> new HashMap<>());

        List<String> list = new ArrayList<>(names);
        list.removeIf(value -> this.describeConfigs.get(clusterId).entrySet()
            .stream()
            .filter(entry -> entry.getKey().type() == type)
            .anyMatch(entry -> entry.getKey().name().equals(value))
        );

        if (list.size() > 0) {
            Map<ConfigResource, Config> description = Logger.call(
                () -> {
                    try {
                        return kafkaModule.getAdminClient(clusterId)
                            .describeConfigs(names.stream()
                                .map(s -> new ConfigResource(type, s))
                                .collect(Collectors.toList())
                            )
                            .all()
                            .get();
                    } catch (ExecutionException e) {
                        if (e.getCause() instanceof SecurityDisabledException || e.getCause() instanceof ClusterAuthorizationException || e.getCause() instanceof TopicAuthorizationException) {
                            return ImmutableMap.of();
                        }

                        if (e.getCause() instanceof ApiException) {
                            throw (ApiException) e.getCause();
                        }

                        throw e;
                    }
                },
                "Describe Topic Config {}",
                names
            );

            this.describeConfigs.get(clusterId).putAll(description);
        }

        return this.describeConfigs
            .get(clusterId)
            .entrySet()
            .stream()
            .filter(e -> e.getKey().type() == type)
            .filter(e -> names.contains(e.getKey().name()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void alterConfigs(String clusterId, Map<ConfigResource, Config> configs) throws ExecutionException {
         Logger.call(
             kafkaModule.getAdminClient(clusterId)
            .alterConfigs(configs)
            .all(),
             "Alter cofigs",
             Collections.singletonList(clusterId)
         );

        this.describeConfigs = new HashMap<>();
    }

    private final Map<String, Map<AclBindingFilter, Collection<AclBinding>>> describeAcls = new HashMap<>();

    public Collection<AclBinding> describeAcls(String clusterId, AclBindingFilter filter) throws ExecutionException, InterruptedException {
        describeAcls.computeIfAbsent(clusterId, s -> new HashMap<>());

        if (!this.describeAcls.get(clusterId).containsKey(filter)) {
            this.describeAcls.get(clusterId).put(filter, Logger.call(
                () -> {
                    try {
                        return kafkaModule.getAdminClient(clusterId)
                            .describeAcls(filter)
                            .values()
                            .get();
                    } catch (ApiException e) {
                        if (e.getCause() instanceof SecurityDisabledException || e.getCause() instanceof ClusterAuthorizationException || e.getCause() instanceof TopicAuthorizationException) {
                            return Collections.emptyList();
                        }

                        if (e.getCause() instanceof ApiException) {
                            throw (ApiException) e.getCause();
                        }

                        throw e;
                    }
                },
                "Describe Acls config",
                null
            ));
        }

        return describeAcls.get(clusterId).get(filter);
    }
}
