package org.akhq.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;

import java.util.Optional;

@ToString
@EqualsAndHashCode
@Getter
public class TopicPartition {
    private final String topic;
    private final int partition;

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
    public static class ConsumerGroupOffset extends TopicPartition {
        private final Optional<Long> offset;
        private final Optional<String> metadata;
        private final Optional<Consumer> member;
        private Optional<Long> firstOffset;
        private Optional<Long> lastOffset;

        public ConsumerGroupOffset(TopicPartition topicPartition) {
            super(topicPartition);

            this.offset = Optional.empty();
            this.metadata = Optional.empty();
            this.member = Optional.empty();
            this.firstOffset = Optional.empty();
            this.lastOffset = Optional.empty();
        }

        public ConsumerGroupOffset(
            org.apache.kafka.common.TopicPartition topicPartition,
            OffsetAndMetadata offsetAndMetadata,
            Partition.Offsets partiionOffsets,
            Consumer member
        ) {
            super(topicPartition);

            this.offset = Optional.of(offsetAndMetadata.offset());
            this.metadata = Optional.of(offsetAndMetadata.metadata());
            this.member = member != null ? Optional.of(member) : Optional.empty() ;
            this.firstOffset = Optional.of(partiionOffsets.getFirstOffset());
            this.lastOffset = Optional.of(partiionOffsets.getLastOffset());
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
