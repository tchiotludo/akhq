package org.akhq.search;

import jakarta.inject.Singleton;
import org.akhq.models.Partition;
import org.akhq.models.Topic;
import org.akhq.repositories.RecordRepository;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class TopicSearchPlanner {
    /**
     * Plans the ranges for an oldest-first scan (search and OLDEST consumption).
     * Each partition starts after its 'after' cursor (or at its first offset / start timestamp)
     * and ends at the end timestamp or the partition end.
     */
    public PartitionRangePlan planOldest(Topic topic, RecordRepository.Options options, KafkaConsumer<byte[], byte[]> consumer) {
        Map<TopicPartition, Long> starts = getFirstOffsets(topic, options, consumer);
        starts.replaceAll((tp, first) -> {
            Long after = options.getAfter().get(tp.partition());
            return after == null ? first : Math.max(first, after + 1);
        });

        return new PartitionRangePlan(buildPartitionRanges(starts, resolveRangeEnds(options, consumer, starts.keySet())));
    }

    /**
     * Plans the ranges for a newest-first scan (NEWEST consumption).
     * Each partition starts at its first offset / start timestamp and ends at the end timestamp or
     * the partition end, capped by its 'after' cursor (exclusive upper bound). Callers read it
     * backward in windows.
     */
    public PartitionRangePlan planNewest(Topic topic, RecordRepository.Options options, KafkaConsumer<byte[], byte[]> consumer) {
        Map<TopicPartition, Long> starts = getFirstOffsets(topic, options, consumer);
        Map<TopicPartition, Long> ends = resolveRangeEnds(options, consumer, starts.keySet());
        ends.replaceAll((tp, end) -> {
            Long after = options.getAfter().get(tp.partition());
            return after == null ? end : Math.min(end, after);
        });

        return new PartitionRangePlan(buildPartitionRanges(starts, ends));
    }

    /**
     * Resolves each partition's exclusive end offset: the first offset after the end timestamp
     * if set, otherwise the partition end.
     */
    private Map<TopicPartition, Long> resolveRangeEnds(
        RecordRepository.Options options,
        KafkaConsumer<byte[], byte[]> consumer,
        Set<TopicPartition> partitions
    ) {
        if (partitions.isEmpty()) {
            return new HashMap<>();
        }

        if (options.getEndTimestamp() == null) {
            return new HashMap<>(consumer.endOffsets(partitions));
        }

        // endTimestamp is inclusive but the range end is exclusive: look up endTimestamp + 1 to keep
        // records at exactly endTimestamp.
        long exclusiveTimestamp = options.getEndTimestamp() == Long.MAX_VALUE
            ? Long.MAX_VALUE
            : options.getEndTimestamp() + 1;
        Map<TopicPartition, Long> ends = new HashMap<>();
        consumer.offsetsForTimes(partitions.stream()
                .collect(Collectors.toMap(Function.identity(), ignored -> exclusiveTimestamp)))
            .forEach((tp, offsetAndTimestamp) -> {
                if (offsetAndTimestamp != null) {
                    ends.put(tp, offsetAndTimestamp.offset());
                }
            });

        // No record after endTimestamp: the range goes up to the partition end.
        Set<TopicPartition> unresolved = partitions.stream()
            .filter(tp -> !ends.containsKey(tp))
            .collect(Collectors.toSet());
        if (!unresolved.isEmpty()) {
            ends.putAll(consumer.endOffsets(unresolved));
        }

        return ends;
    }

    /**
     * Resolves each selected partition's first offset, or the first offset at or after the start
     * timestamp if set. Partitions with no record after the start timestamp are omitted.
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

        if (options.getTimestamp() == null) {
            partitions.forEach(partition ->
                result.put(new TopicPartition(partition.getTopic(), partition.getId()), partition.getFirstOffset()));
            return result;
        }

        Map<TopicPartition, OffsetAndTimestamp> timestampOffsets = consumer.offsetsForTimes(partitions.stream()
            .collect(Collectors.toMap(
                partition -> new TopicPartition(partition.getTopic(), partition.getId()),
                ignored -> options.getTimestamp()
            )));
        timestampOffsets.forEach((tp, offset) -> {
            if (offset != null) {
                result.put(tp, offset.offset());
            }
        });
        return result;
    }

    /**
     * Builds one [begin, end) range per partition, dropping empty ones, sorted by partition.
     */
    public static Map<TopicPartition, PartitionRange> buildPartitionRanges(Map<TopicPartition, Long> starts, Map<TopicPartition, Long> ends) {
        return starts.entrySet().stream()
            .map(entry -> new PartitionRange(entry.getKey(), entry.getValue(), ends.getOrDefault(entry.getKey(), entry.getValue())))
            .filter(range -> range.begin() < range.end())
            .sorted(Comparator.comparingInt(range -> range.topicPartition().partition()))
            .collect(Collectors.toMap(PartitionRange::topicPartition, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * One request's plan: one range per partition to scan.
     */
    public record PartitionRangePlan(Map<TopicPartition, PartitionRange> ranges) {
        public boolean isEmpty() {
            return ranges.isEmpty();
        }

        /**
         * Exclusive end offset of each range.
         */
        public Map<TopicPartition, Long> rangeEnds() {
            return ranges.values().stream()
                .collect(Collectors.toMap(PartitionRange::topicPartition, PartitionRange::end));
        }
    }

    /**
     * One partition's [begin, end) offset range.
     */
    public record PartitionRange(TopicPartition topicPartition, long begin, long end) { }
}
