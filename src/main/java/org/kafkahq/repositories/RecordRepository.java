package org.kafkahq.repositories;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import lombok.*;
import lombok.experimental.Wither;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.jooby.Env;
import org.jooby.Jooby;
import org.kafkahq.models.Partition;
import org.kafkahq.models.Record;
import org.kafkahq.models.Topic;
import org.kafkahq.modules.KafkaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class RecordRepository extends AbstractRepository implements Jooby.Module {
    private KafkaModule kafkaModule;

    private TopicRepository topicRepository;

    private static Logger logger = LoggerFactory.getLogger(RecordRepository.class);

    @Inject
    public RecordRepository(KafkaModule kafkaModule, TopicRepository topicRepository) {
        this.kafkaModule = kafkaModule;
        this.topicRepository = topicRepository;
    }

    public List<Record<String, String>> consume(Options options) throws ExecutionException, InterruptedException {
        return this.kafkaModule.debug(() -> {
            Topic topicsDetail = topicRepository.findByName(options.topic);

            if (options.sort == Options.Sort.OLDEST) {
                return consumeOldest(topicsDetail, options);
            } else {
                return consumeNewest(topicsDetail, options);
            }
        }, "Consume with options {}", options);
    }

    private List<Record<String, String>> consumeOldest(Topic topic, Options options) {
        KafkaConsumer<String, String> consumer = this.kafkaModule.getConsumer(options.clusterId);
        Map<TopicPartition, Long> partitions = getTopicPartitionForSortOldest(topic, options, consumer);
        List<Record<String, String>> list = new ArrayList<>();

        if (partitions.size() > 0) {
            consumer.assign(partitions.keySet());
            partitions.forEach(consumer::seek);

            partitions.forEach((topicPartition, first) ->
                logger.trace(
                    "Consume [topic: {}] [partition: {}] [start: {}]",
                    topicPartition.topic(),
                    topicPartition.partition(),
                    first
                )
            );

            synchronized (consumer) {
                ConsumerRecords<String, String> records = this.poll(consumer);

                for (ConsumerRecord<String, String> record : records) {
                    list.add(new Record<>(record));
                }
            }
        }
        return list;
    }

    public List<TimeOffset> getOffsetForTime(String clusterId, List<org.kafkahq.models.TopicPartition> partitions, Long timestamp) throws ExecutionException, InterruptedException {
        return this.kafkaModule.debug(() -> {
            Map<TopicPartition, Long> map = new HashMap<>();

            KafkaConsumer<String, String> consumer = this.kafkaModule.getConsumer(clusterId);

            partitions
                .forEach(partition -> map.put(
                    new TopicPartition(partition.getTopic(), partition.getPartition()),
                    timestamp
                ));

            return consumer.offsetsForTimes(map)
                .entrySet()
                .stream()
                .map(r -> r.getValue() != null ? new TimeOffset(
                    r.getKey().topic(),
                    r.getKey().partition(),
                    r.getValue().offset()
                ) : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        }, "Offsets for {} Timestamp {}", partitions, timestamp);
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @AllArgsConstructor
    public static class TimeOffset {
        private String topic;
        private int partition;
        private long offset;
    }


    private Map<TopicPartition, Long> getTopicPartitionForSortOldest(Topic topic, Options options, KafkaConsumer<String, String> consumer) {
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

    private List<Record<String, String>> consumeNewest(Topic topic, Options options) {
        int pollSizePerPartition = pollSizePerPartition(topic, options);

        KafkaConsumer<String, String> consumer = this.kafkaModule.getConsumer(
            options.clusterId,
            new Properties() {{
                put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(pollSizePerPartition));
            }}
        );

        return topic
            .getPartitions()
            .stream()
            .map(partition -> getOffsetForSortNewest(consumer, partition, options, pollSizePerPartition)
                .map(offset -> offset.withTopicPartition(
                    new TopicPartition(
                        partition.getTopic(),
                        partition.getId()
                    )
                ))
            )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(topicPartitionOffset -> {
                consumer.assign(Collections.singleton(topicPartitionOffset.getTopicPartition()));
                consumer.seek(topicPartitionOffset.getTopicPartition(), topicPartitionOffset.getBegin());
                List<Record<String, String>> list = new ArrayList<>();


                    int emptyPoll = 0;

                    do {
                        ConsumerRecords<String, String> records;

                        synchronized (consumer) {
                            records = this.poll(consumer);
                        }

                        if (records.isEmpty()) {
                            emptyPoll++;
                        } else {
                            emptyPoll = 0;
                        }

                        for (ConsumerRecord<String, String> record : records) {
                            if (record.offset() > topicPartitionOffset.getEnd()) {
                                emptyPoll = 2;
                                break;
                            }

                            list.add(new Record<>(record));
                        }
                    }
                    while (emptyPoll < 1);

                Collections.reverse(list);

                return Stream.of(list);
            })
             .flatMap(List::stream)
             .collect(Collectors.toList());
    }

    private int pollSizePerPartition(Topic topic, Options options) {
        if (options.partition != null) {
            return options.size;
        } else {
            return (int) Math.round(options.size * 1.0 / topic.getPartitions().size());
        }
    }

    private Optional<Long> getFirstOffset(KafkaConsumer<String, String> consumer, Partition partition, Options options) {
        if (options.partition != null && partition.getId() != options.partition) {
            return Optional.empty();
        }

        long first = partition.getFirstOffset();

        if (options.timestamp != null) {
            Map<TopicPartition, OffsetAndTimestamp> timestampOffset = consumer.offsetsForTimes(
                ImmutableMap.of(
                    new TopicPartition(partition.getTopic(), partition.getId()),
                    options.timestamp
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

    private Optional<OffsetBound> getFirstOffsetForSortOldest(KafkaConsumer<String, String> consumer, Partition partition, Options options) {
        return getFirstOffset(consumer, partition, options)
            .map(first -> {
                if (options.after.size() > 0 && options.after.containsKey(partition.getId())) {
                    first = options.after.get(partition.getId()) + 1;
                }

                if (first > partition.getLastOffset()) {
                    return null;
                }

                return OffsetBound.builder()
                    .begin(first)
                    .build();
            });
    }

    private Optional<EndOffsetBound> getOffsetForSortNewest(KafkaConsumer<String, String> consumer, Partition partition, Options options, int pollSizePerPartition) {
        return getFirstOffset(consumer, partition, options)
            .map(first -> {
                long last = partition.getLastOffset();

                if (pollSizePerPartition > 0 && options.after.containsKey(partition.getId())) {
                    last = options.after.get(partition.getId()) - 1;
                }

                if (last == partition.getFirstOffset() || last < 0) {
                    return null;
                } else if (!(last - pollSizePerPartition < first)) {
                    first = last - pollSizePerPartition;
                }

                return EndOffsetBound.builder()
                    .begin(first)
                    .end(last)
                    .build();
            });
    }

    @SuppressWarnings("deprecation")
    private ConsumerRecords<String, String> poll(KafkaConsumer<String, String> consumer) {
        /*
        // poll with long call poll(final long timeoutMs, boolean includeMetadataInTimeout = true)
        // poll with Duration call poll(final long timeoutMs, boolean includeMetadataInTimeout = false)
        // So second one don't wait for metadata and return empty records
        // First one wait for metadata and send records
        // Hack bellow can be used to wait for metadata
        */
        return consumer.poll(5000);

        /*
        if (!records.isEmpty()) {
            return records;
        }

        Field field = consumer.getClass().getDeclaredField("client");
        field.setAccessible(true);

        ConsumerNetworkClient client = (ConsumerNetworkClient) field.get(consumer);

        while(!client.hasReadyNodes(System.currentTimeMillis())) {
            Thread.sleep(100);
        }

        return consumer.poll(Duration.ofMillis(2000));
        */
    }


    public RecordMetadata produce(String clusterId, String topic, String value, Map<String, String> headers, Optional<String> key, Optional<Integer> partition, Optional<Long> timestamp) throws ExecutionException, InterruptedException {
        return kafkaModule.getProducer(clusterId).send(new ProducerRecord<>(
            topic,
            partition.orElse(null),
            timestamp.orElse(null),
            key.orElse(null),
            value,
            headers
                .entrySet()
                .stream()
                .map(entry -> new RecordHeader(entry.getKey(), entry.getValue().getBytes()))
                .collect(Collectors.toList())
        )).get();
    }

    public void delete(String clusterId, String topic, Integer partition, String key) throws ExecutionException, InterruptedException {
        kafkaModule.getProducer(clusterId).send(new ProducerRecord<>(
            topic,
            partition,
            key,
            null
        )).get();
    }


    public SearchEnd search(Options options, SearchConsumer callback) throws ExecutionException, InterruptedException {
        KafkaConsumer<String, String> consumer = this.kafkaModule.getConsumer(options.clusterId);
        Topic topic = topicRepository.findByName(options.topic);
        Map<TopicPartition, Long> partitions = getTopicPartitionForSortOldest(topic, options, consumer);
        SearchEvent searchEvent = new SearchEvent(topic);

        callback.accept(searchEvent);
        List<Record<String, String>> all = new ArrayList<>();

        int emptyPoll = 0;
        if (partitions.size() > 0) {
            consumer.assign(partitions.keySet());
            partitions.forEach(consumer::seek);
            partitions.forEach((topicPartition, first) ->
                logger.trace(
                    "Search [topic: {}] [partition: {}] [start: {}]",
                    topicPartition.topic(),
                    topicPartition.partition(),
                    first
                )
            );

            synchronized (consumer) {
                List<Record<String, String>> list = new ArrayList<>();

                do {
                    list.removeIf(r -> true);
                    ConsumerRecords<String, String> records = this.poll(consumer);

                    if (records.isEmpty()) {
                        emptyPoll++;
                    } else {
                        emptyPoll = 0;
                    }

                    for (ConsumerRecord<String, String> record : records) {

                        if (callback.isClose()) {
                            break;
                        }

                        searchEvent.updateProgress(record);

                        if (searchFilter(options, record)) {
                            list.add(new Record<>(record));

                            logger.trace(
                                "Record [topic: {}] [partition: {}] [offset: {}] [key: {}]",
                                record.topic(),
                                record.partition(),
                                record.offset(),
                                record.key()
                            );
                        }

                    }

                    if (list.size() > 0) {
                        searchEvent.records = list;
                        all.addAll(list);
                    }

                    callback.accept(searchEvent);
                }
                while (emptyPoll < 1 && all.size() <= options.getSize() && !callback.isClose());
            }
        }

        return new SearchEnd(emptyPoll < 1 ? options.pagination(all) : null);
    }

    private boolean searchFilter(Options options, ConsumerRecord<String, String> record) {
        if (record.key() != null && record.key().toLowerCase().contains(options.getSearch().toLowerCase())) {
            return true;
        }

        if (record.value() != null && record.value().toLowerCase().contains(options.getSearch().toLowerCase())) {
            return true;
        }

        return false;
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    public abstract static class SearchConsumer implements Closeable, Consumer<SearchEvent> {
        private boolean isClose = false;

        @Override
        public void close() throws IOException {
            this.isClose = true;
            logger.info("SearchConsumer closed called");
        }
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchEnd {
        @JsonProperty("after")
        private String after;
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    public static class SearchEvent {
        @JsonProperty("offsets")
        private Map<Integer, Offset> offsets = new HashMap<>();

        @JsonProperty("progress")
        private Map<Integer, Long> progress = new HashMap<>();

        @JsonProperty("records")
        private List<Record<String, String>> records = new ArrayList<>();

        private SearchEvent(Topic topic) {
            topic.getPartitions()
                .forEach(partition -> {
                    offsets.put(partition.getId(), new Offset(partition.getFirstOffset(), partition.getLastOffset()));
                    progress.put(partition.getId(), partition.getLastOffset() - partition.getFirstOffset());
                });
        }

        private void updateProgress(ConsumerRecord<String, String> record) {
            Offset offset = offsets.get(record.partition());
            progress.put(record.partition(), offset.end - offset.begin - record.offset());
        }

        @AllArgsConstructor
        public static class Offset {
            @JsonProperty("begin")
            private final long begin;

            @JsonProperty("end")
            private final long end;
        }
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @Setter
    public static class Options {
        public enum Sort {
            OLDEST,
            NEWEST,
        }
        private String clusterId;
        private String topic;
        private int size = 50;
        private Map<Integer, Long> after = new HashMap<>();
        private Sort sort = Sort.OLDEST;
        private Integer partition;
        private Long timestamp;
        private String search;

        public Options(String clusterId, String topic) {
            this.clusterId = clusterId;
            this.topic = topic;
        }

        public void setAfter(String after) {
            this.after.clear();

            //noinspection UnstableApiUsage
            Splitter.on('_')
                .withKeyValueSeparator('-')
                .split(after)
                .forEach((key, value) -> this.after.put(new Integer(key), new Long(value)));
        }

        public String pagination(List<Record<String, String>> records) {
            Map<Integer, Long> next = new HashMap<>(this.after);
            for (Record<String, String> record : records) {
                if (this.sort == Sort.OLDEST && (!next.containsKey(record.getPartition()) || next.get(record.getPartition()) < record.getOffset())) {
                    next.put(record.getPartition(), record.getOffset());
                } else if (this.sort == Sort.NEWEST && (!next.containsKey(record.getPartition()) || next.get(record.getPartition()) > record.getOffset())) {
                    next.put(record.getPartition(), record.getOffset());
                }
            }

            ArrayList<String> segment = new ArrayList<>();

            for (Map.Entry<Integer, Long> offset : next.entrySet()) {
                segment.add(offset.getKey() + "-" + offset.getValue());
            }

            if (next.size() > 0) {
                return String.join("_", segment);
            }

            return null;
        }

        public URIBuilder after(List<Record<String, String>> records, URIBuilder uri) {
            if (records.size() == 0) {
                return URIBuilder.empty();
            }

            return uri.addParameter("after", pagination(records));
        }

        public URIBuilder before(List<Record<String, String>> records, URIBuilder uri) {
            if (records.size() == 0) {
                return URIBuilder.empty();
            }

            return uri.addParameter("before", pagination(records));
        }
    }

    @Data
    @Builder
    private static class OffsetBound {
        @Wither
        private final TopicPartition topicPartition;
        private final long begin;
    }

    @Data
    @Builder
    private static class EndOffsetBound {
        @Wither
        private final TopicPartition topicPartition;
        private final long begin;
        private final long end;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(RecordRepository.class);
    }
}
