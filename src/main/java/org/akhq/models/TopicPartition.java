package org.akhq.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;

import java.util.Optional;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class TopicPartition {
    private String topic;
    private int partition;

    public TopicPartition(org.apache.kafka.common.TopicPartition topicPartition) {
        this.topic = topicPartition.topic();
        this.partition = topicPartition.partition();
    }

    public TopicPartition(TopicPartition topicPartition) {
        this.topic = topicPartition.getTopic();
        this.partition = topicPartition.getPartition();
    }

    public TopicPartition(String topic, int partition) {
        this.topic = topic;
        this.partition = partition;
    }

    @ToString
    @EqualsAndHashCode(callSuper=true)
    @Getter
    @NoArgsConstructor
    public static class ConsumerGroupOffset extends TopicPartition {
        private Optional<Long> offset;
        private Optional<String> metadata;
        private Optional<Long> firstOffset;
        private Optional<Long> lastOffset;

        public ConsumerGroupOffset(TopicPartition topicPartition) {
            super(topicPartition);

            this.offset = Optional.empty();
            this.metadata = Optional.empty();
            this.firstOffset = Optional.empty();
            this.lastOffset = Optional.empty();
        }

        public ConsumerGroupOffset(
            org.apache.kafka.common.TopicPartition topicPartition,
            OffsetAndMetadata offsetAndMetadata,
            Partition.Offsets partitionOffsets
        ) {
            super(topicPartition);

            this.offset = offsetAndMetadata != null ? Optional.of(offsetAndMetadata.offset()) : Optional.empty();
            this.metadata = offsetAndMetadata != null ? Optional.of(offsetAndMetadata.metadata()) : Optional.empty();
            this.firstOffset = partitionOffsets != null ? Optional.of(partitionOffsets.getFirstOffset()) : Optional.empty();
            this.lastOffset = partitionOffsets != null ? Optional.of(partitionOffsets.getLastOffset()) : Optional.empty();
        }

        public Optional<Long> getOffsetLag() {
            if (this.lastOffset.isPresent() && this.offset.isPresent()) {
                return Optional.of(this.lastOffset.get() - this.offset.get());
            } else {
                return Optional.empty();
            }
        }
    }
}
