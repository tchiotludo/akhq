package org.kafkahq.models;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;

import java.util.Optional;

@ToString
@EqualsAndHashCode
public class TopicPartition {
    public TopicPartition(org.apache.kafka.common.TopicPartition topicPartition) {
        this.topic = topicPartition.topic();
        this.partition = topicPartition.partition();
    }

    public TopicPartition(TopicPartition topicPartition) {
        this.topic = topicPartition.getTopic();
        this.partition = topicPartition.getPartition();
    }

    private final String topic;

    public String getTopic() {
        return topic;
    }

    private final int partition;

    public int getPartition() {
        return partition;
    }

    @ToString
    @EqualsAndHashCode(callSuper=true)
    public static class ConsumerGroupOffset extends TopicPartition {
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

        private final Optional<Long> offset;

        public Optional<Long> getOffset() {
            return offset;
        }

        private final Optional<String> metadata;

        public Optional<String> getMetadata() {
            return metadata;
        }

        private final Optional<Consumer> member;

        public Optional<Consumer> getMember() {
            return member;
        }

        private Optional<Long> firstOffset;

        public Optional<Long> getFirstOffset() {
            return firstOffset;
        }

        private Optional<Long> lastOffset;

        public Optional<Long> getLastOffset() {
            return lastOffset;
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
