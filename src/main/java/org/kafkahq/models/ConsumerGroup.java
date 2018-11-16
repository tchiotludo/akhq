package org.kafkahq.models;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;

import java.util.*;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode
public class ConsumerGroup {
    public ConsumerGroup(
        ConsumerGroupDescription groupDescription,
        Map<org.apache.kafka.common.TopicPartition, OffsetAndMetadata> groupOffset,
        Map<String, List<Partition.Offsets>> topicsOffsets
    ) {
        this.id = groupDescription.groupId();
        this.isSimpleConsumerGroup = groupDescription.isSimpleConsumerGroup();
        this.partitionAssignor = groupDescription.partitionAssignor();
        this.state = groupDescription.state();
        this.coordinator = new Node(groupDescription.coordinator());

        for (MemberDescription member : groupDescription.members()) {
            this.members.add(new Consumer(member));
        }

        for (Map.Entry<org.apache.kafka.common.TopicPartition, OffsetAndMetadata> offset : groupOffset.entrySet()) {
            Partition.Offsets topicOffsets = topicsOffsets.get(offset.getKey().topic())
                .stream()
                .filter(item -> item.getPartition() == offset.getKey().partition())
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                    "Topic Partition Offsets '" + offset.getKey().topic() +
                        "' partition " + offset.getKey().partition() + " doesn't exist for group " + this.id
                ));

            this.offsets.add(new TopicPartition.ConsumerGroupOffset(
                offset.getKey(),
                offset.getValue(),
                topicOffsets,
                this.members
                    .stream()
                    .filter(consumer -> consumer.getAssignments()
                        .stream()
                        .filter(topicPartition ->
                            topicPartition.getPartition() == offset.getKey().partition() &&
                                topicPartition.getTopic().equals(offset.getKey().topic())
                        )
                        .collect(Collectors.toList())
                        .size() > 0
                    )
                    .findFirst()
                    .orElse(null)
            ));
        }

        for (Consumer consumer : this.members) {
            for (TopicPartition assignment : consumer.getAssignments()) {
                long count = this.offsets.stream()
                    .filter(entry -> entry.getTopic().equals(assignment.getTopic()) && entry.getPartition() == assignment.getPartition())
                    .count();

                if (count == 0) {
                    this.offsets.add(new TopicPartition.ConsumerGroupOffset(assignment));
                }
            }
        }

        this.offsets.sort(Comparator
            .comparing(org.kafkahq.models.TopicPartition.ConsumerGroupOffset::getTopic)
            .thenComparingInt(org.kafkahq.models.TopicPartition.ConsumerGroupOffset::getPartition)
        );
    }

    private final String id;

    public String getId() {
        return id;
    }

    private final boolean isSimpleConsumerGroup;

    public boolean isSimpleConsumerGroup() {
        return isSimpleConsumerGroup;
    }

    private final String partitionAssignor;

    public String partitionAssignor() {
        return partitionAssignor;
    }

    private final ConsumerGroupState state;

    public ConsumerGroupState getState() {
        return state;
    }

    private final Node coordinator;

    public Node getCoordinator() {
        return coordinator;
    }

    private final ArrayList<Consumer> members = new ArrayList<>();

    public ArrayList<Consumer> getMembers() {
        return members;
    }

    private final ArrayList<org.kafkahq.models.TopicPartition.ConsumerGroupOffset> offsets = new ArrayList<>();

    public ArrayList<org.kafkahq.models.TopicPartition.ConsumerGroupOffset> getOffsets() {
        return offsets;
    }

    public List<String> getActiveTopics() {
        return this.getMembers()
            .stream()
            .flatMap(consumer -> consumer.getAssignments().stream().map(org.kafkahq.models.TopicPartition::getTopic))
            .distinct()
            .sorted(String::compareToIgnoreCase)
            .collect(Collectors.toList());
    }

    public List<String> getTopics() {
        List<String> list = this.getOffsets()
            .stream()
            .map(org.kafkahq.models.TopicPartition::getTopic)
            .distinct()
            .collect(Collectors.toList());

        list.addAll(this.getActiveTopics());

        return list
            .stream()
            .distinct()
            .sorted(String::compareToIgnoreCase)
            .collect(Collectors.toList());
    }

    public boolean isActiveTopic(String topic) {
        return this.getActiveTopics().contains(topic);
    }

    public long getOffsetLag(String topic) {
        return this.offsets.stream()
            .filter(consumerGroupOffset -> consumerGroupOffset.getTopic().equals(topic))
            .map(org.kafkahq.models.TopicPartition.ConsumerGroupOffset::getOffsetLag)
            .reduce(0L,
                (a1, a2) -> a1 + a2.orElse(0L),
                (a1, a2) -> a1 + a2
            );
    }
}
