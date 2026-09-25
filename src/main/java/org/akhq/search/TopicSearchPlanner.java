package org.akhq.search;

import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.akhq.models.Partition;
import org.akhq.models.Topic;
import org.akhq.repositories.RecordRepository;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class TopicSearchPlanner {
    /**
     * Resolves the per-partition begin/end offsets to scan for one request.
     * Set newestOnly to plan a tail window (used by the regular data endpoint's NEWEST sort);
     * otherwise plans a forward, oldest-first window (used by search and OLDEST consumption).
     */
    public PartitionRangePlan resolvePartitionRangePlan(Topic topic, RecordRepository.Options options, KafkaConsumer<byte[], byte[]> consumer, boolean newestOnly) {
        Map<TopicPartition, Long> starts = newestOnly
            ? getTopicPartitionForSortNewest(topic, options, consumer)
            : getTopicPartitionForSortOldest(topic, options, consumer);
        if (starts.isEmpty()) {
            return PartitionRangePlan.empty();
        }

        Map<TopicPartition, Long> endOffsets = resolveRangeEnds(options, consumer, starts);
        if (newestOnly) {
            Map<TopicPartition, Long> adjustedEnds = new HashMap<>();
            for (Map.Entry<TopicPartition, Long> entry : starts.entrySet()) {
                TopicPartition tp = entry.getKey();
                Partition partition = getPartition(topic, tp.partition());
                if (partition == null) {
                    continue;
                }

                long endExclusive = endOffsets.getOrDefault(tp, partition.getLastOffset());
                if (options.getAfter().containsKey(partition.getId())) {
                    endExclusive = Math.min(endExclusive, options.getAfter().get(partition.getId()));
                }

                adjustedEnds.put(tp, endExclusive);
            }
            return new PartitionRangePlan(buildPartitionRanges(
                adjustStartsForNewest(starts, adjustedEnds, options.getSize()),
                adjustedEnds
            ));
        }

        return new PartitionRangePlan(buildPartitionRanges(starts, endOffsets));
    }

    /**
     * Resolves the exclusive end offset for each partition.
     * With an end timestamp, looks up the first offset at or after endTimestamp + 1 (see below);
     * otherwise falls back to the broker's live end offset.
     */
    private Map<TopicPartition, Long> resolveRangeEnds(
        RecordRepository.Options options,
        KafkaConsumer<byte[], byte[]> consumer,
        Map<TopicPartition, Long> starts
    ) {
        if (options.getEndTimestamp() != null) {
            // endTimestamp is inclusive for the caller, but offsetsForTimes() and the range end are
            // both exclusive, so shift by one ms (guarding against overflow) to keep the boundary record.
            long exclusiveTimestamp = options.getEndTimestamp() == Long.MAX_VALUE
                ? Long.MAX_VALUE
                : options.getEndTimestamp() + 1;
            Map<TopicPartition, Long> endOffsetsToSearch = starts.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), ignored -> exclusiveTimestamp));
            Map<TopicPartition, Long> resolvedEndOffsets = new HashMap<>();
            consumer.offsetsForTimes(endOffsetsToSearch).forEach((tp, offsetAndTimestamp) -> {
                if (offsetAndTimestamp != null) {
                    resolvedEndOffsets.put(tp, offsetAndTimestamp.offset());
                }
            });

            // When after timestamp is after the last message timestamp, offset will be null
            // Therefore we need to use the actual last offset of the partition
            Set<TopicPartition> unresolved = starts.keySet().stream()
                .filter(tp -> !resolvedEndOffsets.containsKey(tp))
                .collect(Collectors.toSet());
            if (!unresolved.isEmpty()) {
                resolvedEndOffsets.putAll(consumer.endOffsets(unresolved));
            }

            return resolvedEndOffsets;
        }

        return new HashMap<>(consumer.endOffsets(starts.keySet()));
    }

    /**
     * Narrows the start of a newest ("tail") window to the last `size` offsets before its end.
     */
    private Map<TopicPartition, Long> adjustStartsForNewest(
        Map<TopicPartition, Long> starts,
        Map<TopicPartition, Long> ends,
        int size
    ) {
        Map<TopicPartition, Long> newestStarts = new HashMap<>();

        for (Map.Entry<TopicPartition, Long> entry : starts.entrySet()) {
            long first = entry.getValue();
            long endExclusive = ends.getOrDefault(entry.getKey(), first);
            if (endExclusive <= first) {
                continue;
            }

            long candidateStart = Math.max(first, endExclusive - size);
            newestStarts.put(entry.getKey(), candidateStart);
        }

        return newestStarts;
    }

    /**
     * Finds the metadata for a partition by id in the topic model.
     */
    private Partition getPartition(Topic topic, int partitionId) {
        return topic.getPartitions().stream()
            .filter(p -> p.getId() == partitionId)
            .findFirst()
            .orElse(null);
    }

    /**
     * Resolves each partition's raw first offset (see getFirstOffsets); the 'after' cursor is applied
     * later, only for the end of the window (see resolvePartitionRangePlan).
     */
    private Map<TopicPartition, Long> getTopicPartitionForSortNewest(
        Topic topic,
        RecordRepository.Options options,
        KafkaConsumer<byte[], byte[]> consumer
    ) {
        return getFirstOffsets(topic, options, consumer);
    }

    /**
     * Resolves the begin offset to scan for each partition, applying the 'after' cursor and
     * dropping partitions with no work left (see getFirstOffsetForSortOldest).
     */
    private Map<TopicPartition, Long> getTopicPartitionForSortOldest(
        Topic topic,
        RecordRepository.Options options,
        KafkaConsumer<byte[], byte[]> consumer
    ) {
        return getFirstOffsets(topic, options, consumer).entrySet().stream()
            .map(entry -> getFirstOffsetForSortOldest(
                entry.getKey(),
                entry.getValue(),
                topic,
                options
            ))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(OffsetBound::getTopicPartition, OffsetBound::getBegin));
    }

    /**
     * Resolves each selected partition's first offset: partition's first offset, or (if a start
     * timestamp is set) the first offset at or after it, resolved in one batched offsetsForTimes call.
     * Partitions with no match for the timestamp are omitted.
     */
    private Map<TopicPartition, Long> getFirstOffsets(
        Topic topic,
        RecordRepository.Options options,
        KafkaConsumer<byte[], byte[]> consumer
    ) {
        List<Partition> partitions = topic.getPartitions().stream()
            .filter(partition -> options.getPartition() == null || partition.getId() == options.getPartition())
            .toList();
        Map<TopicPartition, Long> result = new HashMap<>();
        Map<TopicPartition, Long> timestampRequests = options.getTimestamp() == null
            ? Collections.emptyMap()
            : partitions.stream().collect(Collectors.toMap(
                partition -> new TopicPartition(partition.getTopic(), partition.getId()),
                ignored -> options.getTimestamp()
            ));
        Map<TopicPartition, OffsetAndTimestamp> timestampOffsets = options.getTimestamp() == null
            ? Collections.emptyMap()
            : consumer.offsetsForTimes(timestampRequests);

        for (Partition partition : partitions) {
            TopicPartition tp = new TopicPartition(partition.getTopic(), partition.getId());
            OffsetAndTimestamp offset = timestampOffsets.get(tp);
            if (options.getTimestamp() == null) {
                result.put(tp, partition.getFirstOffset());
            } else if (offset != null) {
                result.put(tp, offset.offset());
            }
        }
        return result;
    }

    /**
     * Applies the 'after' cursor to a partition's first offset, and drops it if there is no more
     * data past that cursor.
     */
    private Optional<OffsetBound> getFirstOffsetForSortOldest(
        TopicPartition tp,
        long resolvedFirst,
        Topic topic,
        RecordRepository.Options options
    ) {
        Partition partition = getPartition(topic, tp.partition());
        long first = resolvedFirst;
        if (options.getAfter().containsKey(partition.getId())) {
            first = options.getAfter().get(partition.getId()) + 1;
        }
        if (first > partition.getLastOffset()) {
            return Optional.empty();
        }
        return Optional.of(OffsetBound.builder().topicPartition(tp).begin(first).build());
    }

    /**
     * Builds one [begin, end) range per partition, dropping empty/exhausted ones, sorted by partition.
     */
    public static Map<TopicPartition, PartitionRange> buildPartitionRanges(Map<TopicPartition, Long> starts, Map<TopicPartition, Long> ends) {
        return starts.entrySet().stream()
            .map(entry -> {
                long begin = entry.getValue();
                long end = ends.getOrDefault(entry.getKey(), begin);
                return new PartitionRange(entry.getKey(), begin, end);
            })
            .filter(range -> range.begin() < range.end())
            .sorted(Comparator.comparingLong(range -> range.topicPartition().partition()))
            .collect(Collectors.toMap(PartitionRange::topicPartition, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * One request's resolved plan: one range per partition to scan.
     */
    public record PartitionRangePlan(Map<TopicPartition, PartitionRange> ranges) {
        /**
         * Plan used when the request has no partition left to scan.
         */
        static PartitionRangePlan empty() {
            return new PartitionRangePlan(Collections.emptyMap());
        }

        /**
         * True when no partition has a range to scan.
         */
        public boolean isEmpty() {
            return ranges.isEmpty();
        }

        /**
         * End offset of each range, keyed by partition.
         */
        public Map<TopicPartition, Long> getRangeEnds() {
            return ranges.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().end()));
        }
    }

    /**
     * One partition's [begin, end) offset range to scan.
     */
    public record PartitionRange(TopicPartition topicPartition, long begin, long end) { }

    /**
     * Holds a partition's begin offset until its TopicPartition key is attached.
     */
    @Getter
    @AllArgsConstructor
    @lombok.Builder
    private static class OffsetBound {
        private TopicPartition topicPartition;
        private final long begin;

    }
}
