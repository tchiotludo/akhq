package org.akhq.mcp.services;

import jakarta.inject.Singleton;
import org.akhq.mcp.model.SearchTopicsArguments;
import org.akhq.mcp.model.SearchTopicsResult;
import org.akhq.mcp.model.TopicOverview;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.repositories.TopicRepository;
import org.apache.kafka.clients.admin.TopicDescription;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Singleton
public class AkhqTopicToolService {
    static final int DEFAULT_MAX_RESULTS = 50;
    static final int MAX_ALLOWED_RESULTS = 200;

    private final TopicRepository topicRepository;
    private final AbstractKafkaWrapper kafkaWrapper;

    public AkhqTopicToolService(TopicRepository topicRepository, AbstractKafkaWrapper kafkaWrapper) {
        this.topicRepository = topicRepository;
        this.kafkaWrapper = kafkaWrapper;
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
