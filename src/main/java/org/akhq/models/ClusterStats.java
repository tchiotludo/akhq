package org.akhq.models;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.akhq.repositories.ConnectRepository;
import org.akhq.repositories.ConsumerGroupRepository;
import org.akhq.repositories.SchemaRegistryRepository;
import org.akhq.repositories.TopicRepository;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClusterStats
{
    private String id;
    private Integer nodeCount;
    private TopicStats topicStats;
    private ConsumerGroupStats consumerGroupStats;
    private List<ConnectStats> connectStats;
    private SchemaRegistryStats schemaRegistryStats;

    public ClusterStats(String id, TopicRepository tr, ConsumerGroupRepository cgr, ConnectRepository cr, SchemaRegistryRepository srr)
            throws ExecutionException, InterruptedException, IOException, RestClientException
    {
        this.id = id;
        this.topicStats = tr.getTopicStats(id);
        this.consumerGroupStats = cgr.getConsumerGroupStats(id);
        this.connectStats = cr.getConnectStats(id);
        this.schemaRegistryStats = srr.schemaCount(id);
    }

    @ToString
    @Getter
    @AllArgsConstructor
    public static class TopicStats {
        private final Integer topics;
        private final Integer partitions;
        private final Integer replicaCount;
        private final Integer inSyncReplicaCount;
    }

    @ToString
    @Getter
    @AllArgsConstructor
    public static class ConsumerGroupStats {
        private final Integer consumerGroups;
        private final Integer rebalancingGroups;
        private final Integer emptyGroups;
    }

    @ToString
    @Getter
    @AllArgsConstructor
    public static class ConnectStats {
        private final String connectId;
        private final Integer connectors;
        private final Integer tasks;
        private final Map<String, Long> stateCount;
    }

    @ToString
    @Getter
    @AllArgsConstructor
    public static class SchemaRegistryStats {
        private final Integer schemas;
    }

}
