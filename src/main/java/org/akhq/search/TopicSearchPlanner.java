package org.akhq.search;

import com.google.common.collect.ImmutableMap;
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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class TopicSearchPlanner {
    /**
     * Builds the bounded search window that each partition should scan for a single request.
     * This planner is the main optimization point for AKHQ topic search on large or sparse Kafka
     * topics. Instead of blindly polling every partition from the beginning or from a stale guess,
     * we resolve a start offset and an end offset for each partition once, then reuse that plan for
     * the actual consumer iteration. This keeps the search path predictable for topics with a single
     * partition and a tiny amount of data, but also prevents runaway scans on topics with millions of
     * records spread across many partitions.
     * Product-wise the key rule is: when the user is searching a topic, prefer a timestamp window to
     * an open-ended scan. Timestamps are the safest way to keep search bounded on a generic product
     * where topics can be tiny or extremely large, and where some partitions may be empty while others
     * are dense. This planner centralizes that logic so the rest of the repository can stay focused on
     * consumer orchestration rather than on offset math.
     */
    public PartitionRangePlan resolvePartitionRangePlan(Topic topic, RecordRepository.Options options, KafkaConsumer<byte[], byte[]> consumer, boolean newestOnly) {
        Map<TopicPartition, Long> starts = getTopicPartitionForSortOldest(topic, options, consumer);
        if (starts.isEmpty()) {
            return PartitionRangePlan.empty();
        }

        Map<TopicPartition, Long> endOffsets = resolveRangeEnds(options, consumer, starts);
        Map<TopicPartition, Long> adjustedStarts = newestOnly ? adjustStartsForNewest(topic, options, starts) : starts;

        if (newestOnly) {
            Map<TopicPartition, Long> adjustedEnds = new HashMap<>();
            for (Map.Entry<TopicPartition, Long> entry : adjustedStarts.entrySet()) {
                TopicPartition tp = entry.getKey();
                Partition partition = getPartition(topic, tp.partition());
                if (partition == null) {
                    continue;
                }
                long last = partition.getLastOffset() - 1;
                adjustedEnds.put(tp, endOffsets.getOrDefault(tp, last));
            }
            endOffsets = adjustedEnds;
        }

        return new PartitionRangePlan(buildPartitionRanges(adjustedStarts, endOffsets));
    }

    /**
     * Resolves the end offset for each partition in the search window.
     * When an end timestamp is provided, we ask Kafka for the first offset at or after that moment.
     * This is the most useful case for user-driven searches because it keeps the search bounded even
     * when a partition is very large. If there is no end timestamp, we fall back to the broker's live
     * end offset for each partition.
     */
    private Map<TopicPartition, Long> resolveRangeEnds(
        RecordRepository.Options options,
        KafkaConsumer<byte[], byte[]> consumer,
        Map<TopicPartition, Long> starts
    ) {
        if (options.getEndTimestamp() != null) {
            Map<TopicPartition, Long> endOffsetsToSearch = starts.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), ignored -> options.getEndTimestamp()));
            Map<TopicPartition, Long> resolvedEndOffsets = new HashMap<>();
            consumer.offsetsForTimes(endOffsetsToSearch).forEach((tp, offsetAndTimestamp) -> {
                if (offsetAndTimestamp != null) {
                    resolvedEndOffsets.put(tp, offsetAndTimestamp.offset());
                }
            });
            return resolvedEndOffsets;
        }

        return new HashMap<>(consumer.endOffsets(starts.keySet()));
    }

    /**
     * Narrow the computed start offsets when the request asks for the most recent records only.
     * This behaves like a windowed "tail" search: we keep the last N records of each partition,
     * bounded by the current partition end and the optional 'after' offset. This preserves the
     * semantics of "newest only" while avoiding a full partition scan when the topic is large.
     */
    private Map<TopicPartition, Long> adjustStartsForNewest(Topic topic, RecordRepository.Options options, Map<TopicPartition, Long> starts) {
        Map<TopicPartition, Long> newestStarts = new HashMap<>();

        for (Map.Entry<TopicPartition, Long> entry : starts.entrySet()) {
            Partition partition = getPartition(topic, entry.getKey().partition());
            if (partition == null) {
                continue;
            }

            long last = partition.getLastOffset() - 1;
            if (options.getAfter().containsKey(partition.getId())) {
                last = options.getAfter().get(partition.getId()) - 1;
            }
            if (last < 0) {
                continue;
            }

            long first = entry.getValue();
            if (!(last - options.getSize() < first)) {
                first = last - options.getSize() + 1;
            }

            newestStarts.put(entry.getKey(), first);
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
     * Resolves the first valid offset to start scanning for each partition.
     * The logic is intentionally defensive: if a partition is filtered out by the request's selected
     * partition id, we skip it. If the request includes a timestamp, we convert it to the first Kafka
     * offset at or after that timestamp. If the request includes an explicit 'after' offset, we move the
     * candidate beginning past that marker. At the end, partitions whose first valid offset is already
     * beyond the last offset are discarded because they contain no work to do.
     */
    private Map<TopicPartition, Long> getTopicPartitionForSortOldest(
        Topic topic,
        RecordRepository.Options options,
        KafkaConsumer<byte[], byte[]> consumer
    ) {
        return topic
            .getPartitions()
            .stream()
            .map(partition -> getFirstOffsetForSortOldest(consumer, partition, options)
                .map(offsetBound -> offsetBound.withTopicPartition(
                    new TopicPartition(
                        partition.getTopic(),
                        partition.getId()
                    )
                ))
            )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(OffsetBound::getTopicPartition, OffsetBound::getBegin));
    }

    /**
     * Computes the first valid offset to read for one partition.
     * In a timestamp-based search, this depends on Kafka's offsetsForTimes call. That call is the main
     * guardrail for large topics because it moves the search window from an unbounded idea ("start from
     * the beginning") to a concrete offset boundary derived from the user's time requirement.
     */
    private Optional<Long> getFirstOffset(KafkaConsumer<byte[], byte[]> consumer, Partition partition, RecordRepository.Options options) {
        if (options.getPartition() != null && partition.getId() != options.getPartition()) {
            return Optional.empty();
        }

        long first = partition.getFirstOffset();

        if (options.getTimestamp() != null) {
            Map<TopicPartition, OffsetAndTimestamp> timestampOffset = consumer.offsetsForTimes(
                ImmutableMap.of(
                    new TopicPartition(partition.getTopic(), partition.getId()),
                    options.getTimestamp()
                )
            );

            for (Map.Entry<TopicPartition, OffsetAndTimestamp> entry : timestampOffset.entrySet()) {
                if (entry.getValue() == null) {
                    return Optional.empty();
                }
                first = entry.getValue().offset();
            }
        }

        return Optional.of(first);
    }

    /**
     * Converts the partition start candidate into a real search boundary for the request.
     * This is where we honor the request's 'after' cursor, skip empty partitions, and ensure that the
     * search never asks Kafka to read a range whose beginning is already past the last known offset.
     */
    private Optional<OffsetBound> getFirstOffsetForSortOldest(KafkaConsumer<byte[], byte[]> consumer, Partition partition, RecordRepository.Options options) {
        return getFirstOffset(consumer, partition, options)
            .map(first -> {
                if (!options.getAfter().isEmpty() && options.getAfter().containsKey(partition.getId())) {
                    first = options.getAfter().get(partition.getId()) + 1;
                }

                if (first > partition.getLastOffset()) {
                    return null;
                }

                return OffsetBound.builder()
                    .begin(first)
                    .build();
            });
    }

    /**
     * Builds the final per-partition search windows from the resolved start and end offsets.
     * Each range represents a narrow slice of one partition that should be consumed. We keep only
     * ranges where begin < end because an empty or already-exhausted partition is not useful work. The
     * ranges are sorted by partition number to keep a deterministic scan order.
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
     * Represents the complete range plan for one search request.
     * The planner resolves one range per Kafka partition, and each range is intentionally narrow: it is
     * the slice of records we believe is relevant for the request. Keeping this plan immutable and
     * reusable makes it easier to reason about the search flow, avoid duplicated broker calls, and keep
     * performance stable when the request spans empty partitions or large high-volume partitions.
     */
    public record PartitionRangePlan(Map<TopicPartition, PartitionRange> ranges) {
        /**
         * Empty plan used when a request is filtered out before any real work is possible.
         */
        static PartitionRangePlan empty() {
            return new PartitionRangePlan(Collections.emptyMap());
        }

        /**
         * True when no partition has a meaningful range to scan.
         */
        public boolean isEmpty() {
            return ranges.isEmpty();
        }

        /**
         * Exposes the end offset of each range, which is useful for follow-up filtering or consumer
         * bookkeeping without re-deriving the same information.
         */
        public Map<TopicPartition, Long> getRangeEnds() {
            return ranges.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().end()));
        }
    }

    /**
     * A single Kafka partition range, expressed as begin/end offsets.
     * This is the unit of work we give to the consumer loop. It avoids scanning whole partitions when
     * only a small window is relevant; it also allows the system to skip empty or already-consumed
     * windows without leaving the search logic in the repository.
     */
    public record PartitionRange(TopicPartition topicPartition, long begin, long end) { }

    /**
     * Temporary container used while computing the start boundary for a partition.
     * The planner first computes a begin offset and only then attaches the TopicPartition metadata,
     * which keeps the code simple while still producing the final map that the range plan needs.
     */
    @Getter
    @AllArgsConstructor
    @lombok.Builder
    private static class OffsetBound {
        private TopicPartition topicPartition;
        private final long begin;

        OffsetBound withTopicPartition(TopicPartition topicPartition) {
            return new OffsetBound(topicPartition, begin);
        }
    }
}
