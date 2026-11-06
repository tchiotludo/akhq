package org.akhq.mcp.services;

import jakarta.inject.Singleton;
import org.akhq.mcp.model.GetConfigsResult;
import org.akhq.mcp.model.GetTopicResult;
import org.akhq.mcp.model.SearchTopicsArguments;
import org.akhq.mcp.model.SearchTopicsResult;
import org.akhq.mcp.model.TopicOverview;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.models.Partition;
import org.akhq.models.Topic;
import org.akhq.repositories.ConfigRepository;
import org.akhq.repositories.TopicRepository;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.clients.admin.TopicDescription;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Singleton
public class AkhqTopicToolService {
    static final int DEFAULT_MAX_RESULTS = 50;
    static final int MAX_ALLOWED_RESULTS = 200;

    private final TopicRepository topicRepository;
    private final AbstractKafkaWrapper kafkaWrapper;
    private final ConfigRepository configRepository;

    public AkhqTopicToolService(TopicRepository topicRepository, AbstractKafkaWrapper kafkaWrapper, ConfigRepository configRepository) {
        this.topicRepository = topicRepository;
        this.kafkaWrapper = kafkaWrapper;
        this.configRepository = configRepository;
    }

    public GetTopicResult getTopic(String cluster, String name, Integer maxResults) throws ExecutionException, InterruptedException {
        int limit = McpLimits.maxItems(maxResults);
        Topic topic = findTopic(cluster, name);
        List<Partition> sorted = topic.getPartitions().stream()
            .sorted(Comparator.comparingInt(Partition::getId))
            .toList();
        List<GetTopicResult.Partition> returned = sorted.stream()
            .limit(limit)
            .map(partition -> new GetTopicResult.Partition(
                partition.getId(),
                partition.getLeader().getId(),
                partition.getFirstOffset(),
                partition.getLastOffset(),
                partition.getLastOffset() - partition.getFirstOffset(),
                partition.getLogDirSize(),
                partition.getNodes().stream()
                    .sorted(Comparator.comparingInt(org.akhq.models.Node::getId))
                    .map(node -> new GetTopicResult.Replica(node.getId(), node.isLeader(), node.isInSyncReplicas()))
                    .toList()
            ))
            .toList();
        boolean truncated = sorted.size() > returned.size();
        int underReplicated = (int) sorted.stream()
            .filter(partition -> partition.getNodes().stream().anyMatch(node -> !node.isInSyncReplicas()))
            .count();

        return new GetTopicResult(
            cluster, topic.getName(), topic.isInternalTopic(), sorted.size(), topic.getReplicaCount(),
            underReplicated, topic.getSize(), topic.getLogDirSize(), truncated, returned,
            truncated
                ? "Topic has " + sorted.size() + " partitions, returning the first " + returned.size() + ". Raise `maxResults` to see the others."
                : "Topic has " + sorted.size() + " partition(s)."
        );
    }

    public GetConfigsResult getTopicConfigs(String cluster, String name, String search, Boolean onlyOverridden, Integer maxResults)
        throws ExecutionException, InterruptedException {
        try {
            return McpConfigs.result(
                cluster, "TOPIC", name, configRepository.findByTopic(cluster, name), search, onlyOverridden, maxResults
            );
        } catch (ExecutionException e) {
            throw notFoundOrRethrow(e, name);
        }
    }

    private Topic findTopic(String cluster, String name) throws ExecutionException, InterruptedException {
        try {
            return topicRepository.findByName(cluster, name);
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("Topic '" + name + "' doesn't exist", e);
        } catch (ExecutionException e) {
            throw notFoundOrRethrow(e, name);
        }
    }

    private static ExecutionException notFoundOrRethrow(ExecutionException e, String name) throws ExecutionException {
        if (e.getCause() instanceof UnknownTopicOrPartitionException) {
            throw new IllegalArgumentException("Topic '" + name + "' doesn't exist", e);
        }

        throw e;
    }

    /**
     * @param cluster         the authorized cluster
     * @param resourceFilters the topic name patterns the caller is restricted to, empty for no restriction
     */
    public SearchTopicsResult searchTopics(String cluster, List<String> resourceFilters, SearchTopicsArguments arguments)
        throws ExecutionException, InterruptedException {
        Optional<String> search = Optional.ofNullable(arguments.search())
            .map(String::trim)
            .filter(value -> !value.isEmpty());
        int maxResults = maxResults(arguments.maxResults());

        List<String> names = topicRepository.all(cluster, TopicRepository.TopicListView.ALL, search, resourceFilters);
        List<String> returned = names.subList(0, Math.min(maxResults, names.size()));
        Map<String, TopicDescription> descriptions = returned.isEmpty()
            ? Map.of()
            : kafkaWrapper.describeTopics(cluster, returned);

        List<TopicOverview> topics = returned.stream()
            .map(name -> overview(name, descriptions.get(name)))
            .toList();
        boolean truncated = names.size() > returned.size();

        return new SearchTopicsResult(cluster, names.size(), truncated, topics, message(names.size(), topics.size(), truncated));
    }

    private static TopicOverview overview(String name, TopicDescription description) {
        return new TopicOverview(name, description == null ? 0 : description.partitions().size());
    }

    private static int maxResults(Integer requested) {
        if (requested == null) {
            return DEFAULT_MAX_RESULTS;
        }
        if (requested < 1) {
            throw new IllegalArgumentException("`arguments.maxResults` must be greater than 0");
        }

        return Math.min(requested, MAX_ALLOWED_RESULTS);
    }

    private static String message(int total, int returned, boolean truncated) {
        if (total == 0) {
            return "No topic found. The cluster may have no matching topic, or you may not be allowed to see it.";
        }
        if (truncated) {
            return "Found " + total + " topics, returning the first " + returned
                + " sorted by name. Refine `search` or raise `maxResults` to see the others.";
        }

        return "Found " + total + " topic(s).";
    }
}
