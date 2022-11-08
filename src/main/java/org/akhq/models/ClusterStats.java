package org.akhq.models;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClusterStats
{
    private String id;

    @ToString
    @Getter
    @AllArgsConstructor
    public static class TopicStats
    {
        private final Integer topics;
        private final Integer partitions;
        private final Integer replicaCount;
        private final Integer inSyncReplicaCount;
    }

    @ToString
    @Getter
    @AllArgsConstructor
    public static class ConsumerGroupStats
    {
        private final Integer consumerGroups;
        private final Integer rebalancingGroups;
        private final Integer emptyGroups;
    }

    @ToString
    @Getter
    @AllArgsConstructor
    public static class ConnectStats
    {
        private final String connectId;
        private final Integer connectors;
        private final Integer tasks;
        private final Map<String, Integer> stateCount;
    }

    @ToString
    @Getter
    @AllArgsConstructor
    public static class SchemaRegistryStats
    {
        private final Integer schemas;
    }
}
