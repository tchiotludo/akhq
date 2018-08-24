package org.kafkahq.models;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.kafka.clients.admin.MemberDescription;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode
public class Consumer {
    public Consumer(MemberDescription description) {
        this.id = description.consumerId();
        this.clientId = description.clientId();
        this.host = description.host();

        for (org.apache.kafka.common.TopicPartition assignment : description.assignment().topicPartitions()) {
            this.assignments.add(new TopicPartition(assignment));
        }

        this.assignments.sort(Comparator
            .comparing(org.kafkahq.models.TopicPartition::getTopic)
            .thenComparingInt(org.kafkahq.models.TopicPartition::getPartition)
        );
    }

    private final String id;

    public String getId() {
        return id;
    }

    private final String clientId;

    public String getClientId() {
        return clientId;
    }

    private final String host;

    public String getHost() {
        return host;
    }

    private final ArrayList<TopicPartition> assignments = new ArrayList<>();

    public ArrayList<TopicPartition> getAssignments() {
        return assignments;
    }

    public ArrayList<GroupedAssignement> getGroupedAssignments() {
        Map<String, List<TopicPartition>> collect = this.assignments
            .stream()
            .collect(Collectors.groupingBy(TopicPartition::getTopic))
        ;

        ArrayList<GroupedAssignement> list = new ArrayList<>();

        for(Map.Entry<String, List<TopicPartition>> item : collect.entrySet()) {
            list.add(new GroupedAssignement(item.getKey(), item.getValue().stream().mapToInt(TopicPartition::getPartition).toArray()));
        }

        return list;
    }

    public static class GroupedAssignement {
        private GroupedAssignement (String topic, int[] partitions) {
            this.topic = topic;
            this.partitions = partitions;
        }

        private final String topic;

        public String getTopic() {
            return topic;
        }

        private final int[] partitions;

        public int[] getPartitions() {
            return partitions;
        }
    }
}
