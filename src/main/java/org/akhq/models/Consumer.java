package org.akhq.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.kafka.clients.admin.MemberDescription;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class Consumer {
    private String id;
    private String clientId;
    private String host;
    private final ArrayList<TopicPartition> assignments = new ArrayList<>();

    public Consumer(MemberDescription description) {
        this.id = description.consumerId();
        this.clientId = description.clientId();
        this.host = description.host();

        for (org.apache.kafka.common.TopicPartition assignment : description.assignment().topicPartitions()) {
            this.assignments.add(new TopicPartition(assignment));
        }

        this.assignments.sort(Comparator
            .comparing(org.akhq.models.TopicPartition::getTopic)
            .thenComparingInt(org.akhq.models.TopicPartition::getPartition)
        );
    }

    @JsonIgnore
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

    @ToString
    @EqualsAndHashCode
    @Getter
    public static class GroupedAssignement {
        private final String topic;
        private final int[] partitions;

        private GroupedAssignement (String topic, int[] partitions) {
            this.topic = topic;
            this.partitions = partitions;
        }
    }
}
