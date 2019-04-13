package org.kafkahq.repositories;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.http.sse.Event;
import io.reactivex.Flowable;
import lombok.*;
import lombok.experimental.Wither;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.kafkahq.models.Partition;
import org.kafkahq.models.Record;
import org.kafkahq.models.Topic;
import org.kafkahq.modules.KafkaModule;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@Slf4j
public class RecordRepository extends AbstractRepository {
    private final KafkaModule kafkaModule;
    private final TopicRepository topicRepository;
    private final SchemaRegistryRepository schemaRegistryRepository;

    @Value("${kafkahq.topic-data.poll-timeout}")
    protected int pollTimeout;

    @Inject
    public RecordRepository(KafkaModule kafkaModule, TopicRepository topicRepository, SchemaRegistryRepository schemaRegistryRepository) {
        this.kafkaModule = kafkaModule;
        this.topicRepository = topicRepository;
        this.schemaRegistryRepository = schemaRegistryRepository;
    }

    public List<Record> consume(Options options) throws ExecutionException, InterruptedException {
        return this.kafkaModule.debug(() -> {
            Topic topicsDetail = topicRepository.findByName(options.topic);

            if (options.sort == Options.Sort.OLDEST) {
                return consumeOldest(topicsDetail, options);
            } else {
                return consumeNewest(topicsDetail, options);
            }
        }, "Consume with options {}", options);
    }

    private List<Record> consumeOldest(Topic topic, Options options) {
        KafkaConsumer<byte[], byte[]> consumer = this.kafkaModule.getConsumer(options.clusterId);
        Map<TopicPartition, Long> partitions = getTopicPartitionForSortOldest(topic, options, consumer);
        List<Record> list = new ArrayList<>();

        if (partitions.size() > 0) {
            consumer.assign(partitions.keySet());
            partitions.forEach(consumer::seek);

            partitions.forEach((topicPartition, first) ->
                log.trace(
                    "Consume [topic: {}] [partition: {}] [start: {}]",
                    topicPartition.topic(),
                    topicPartition.partition(),
                    first
                )
            );

            ConsumerRecords<byte[], byte[]> records = this.poll(consumer);

            for (ConsumerRecord<byte[], byte[]> record : records) {
                list.add(newRecord(record, options));
            }
        }

        consumer.close();

        return list;
    }

    public List<TimeOffset> getOffsetForTime(String clusterId, List<org.kafkahq.models.TopicPartition> partitions, Long timestamp) throws ExecutionException, InterruptedException {
        return this.kafkaModule.debug(() -> {
            Map<TopicPartition, Long> map = new HashMap<>();

            KafkaConsumer<byte[], byte[]> consumer = this.kafkaModule.getConsumer(clusterId);

            partitions
                .forEach(partition -> map.put(
                    new TopicPartition(partition.getTopic(), partition.getPartition()),
                    timestamp
                ));

            List<TimeOffset> collect = consumer.offsetsForTimes(map)
                .entrySet()
                .stream()
                .map(r -> r.getValue() != null ? new TimeOffset(
                    r.getKey().topic(),
                    r.getKey().partition(),
                    r.getValue().offset()
                ) : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            consumer.close();

            return collect;

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


    private Map<TopicPartition, Long> getTopicPartitionForSortOldest(Topic topic, Options options, KafkaConsumer<byte[], byte[]> consumer) {
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

    private List<Record> consumeNewest(Topic topic, Options options) {
        int pollSizePerPartition = pollSizePerPartition(topic, options);

        return topic
            .getPartitions()
            .parallelStream()
            .map(partition -> {
                KafkaConsumer<byte[], byte[]> consumer = this.kafkaModule.getConsumer(
                    options.clusterId,
                    new Properties() {{
                        put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(pollSizePerPartition));
                    }}
                );

                return getOffsetForSortNewest(consumer, partition, options, pollSizePerPartition)
                        .map(offset -> offset.withTopicPartition(
                            new TopicPartition(
                                partition.getTopic(),
                                partition.getId()
                            )
                        ));
                }
            )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(topicPartitionOffset -> {
                topicPartitionOffset.getConsumer().assign(Collections.singleton(topicPartitionOffset.getTopicPartition()));
                topicPartitionOffset.getConsumer().seek(topicPartitionOffset.getTopicPartition(), topicPartitionOffset.getBegin());

                List<Record> list = new ArrayList<>();
                int emptyPoll = 0;

                do {
                    ConsumerRecords<byte[], byte[]> records;

                    records = this.poll(topicPartitionOffset.getConsumer());

                    if (records.isEmpty()) {
                        emptyPoll++;
                    } else {
                        emptyPoll = 0;
                    }

                    for (ConsumerRecord<byte[], byte[]> record : records) {
                        if (record.offset() > topicPartitionOffset.getEnd()) {
                            emptyPoll = 2;
                            break;
                        }

                        list.add(newRecord(record, options));
                    }
                }
                while (emptyPoll < 1);

                Collections.reverse(list);

                topicPartitionOffset.getConsumer().close();

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

    private Optional<Long> getFirstOffset(KafkaConsumer<byte[], byte[]> consumer, Partition partition, Options options) {
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

    private Optional<OffsetBound> getFirstOffsetForSortOldest(KafkaConsumer<byte[], byte[]> consumer, Partition partition, Options options) {
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

    private Optional<EndOffsetBound> getOffsetForSortNewest(KafkaConsumer<byte[], byte[]> consumer, Partition partition, Options options, int pollSizePerPartition) {
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
                    .consumer(consumer)
                    .begin(first)
                    .end(last)
                    .build();
            });
    }

    @SuppressWarnings("deprecation")
    private ConsumerRecords<byte[], byte[]> poll(KafkaConsumer<byte[], byte[]> consumer) {
        /*
        // poll with long call poll(final long timeoutMs, boolean includeMetadataInTimeout = true)
        // poll with Duration call poll(final long timeoutMs, boolean includeMetadataInTimeout = false)
        // So second one don't wait for metadata and return empty records
        // First one wait for metadata and send records
        // Hack bellow can be used to wait for metadata
        */
        return consumer.poll(this.pollTimeout);

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

    private Record newRecord(ConsumerRecord<byte[], byte[]> record, Options options) {
        return new Record(record, this.schemaRegistryRepository.getKafkaAvroDeserializer(options.clusterId));
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

    public void delete(String clusterId, String topic, Integer partition, byte[] key) throws ExecutionException, InterruptedException {
        kafkaModule.getProducer(clusterId).send(new ProducerRecord<>(
            topic,
            partition,
            new String(key),
            null
        )).get();
    }

    public Flowable<Event<SearchEvent>> search(Options options) throws ExecutionException, InterruptedException {
        KafkaConsumer<byte[], byte[]> consumer = this.kafkaModule.getConsumer(options.clusterId);
        Topic topic = topicRepository.findByName(options.topic);
        Map<TopicPartition, Long> partitions = getTopicPartitionForSortOldest(topic, options, consumer);

        AtomicInteger matchesCount = new AtomicInteger();

        if (partitions.size() == 0) {
            return Flowable.just(new SearchEvent(topic).end());
        }

        consumer.assign(partitions.keySet());
        partitions.forEach(consumer::seek);

        partitions.forEach((topicPartition, first) ->
            log.trace(
                "Search [topic: {}] [partition: {}] [start: {}]",
                topicPartition.topic(),
                topicPartition.partition(),
                first
            )
        );

        return Flowable.generate(() -> new SearchEvent(topic), (searchEvent, emitter) -> {
            // end
            if (searchEvent.emptyPoll == 666) {
                emitter.onComplete();
                consumer.close();

                return searchEvent;
            }

            SearchEvent currentEvent = new SearchEvent(searchEvent);

            ConsumerRecords<byte[], byte[]> records = this.poll(consumer);

            if (records.isEmpty()) {
                currentEvent.emptyPoll++;
            } else {
                currentEvent.emptyPoll = 0;
            }

            List<Record> list = new ArrayList<>();

            for (ConsumerRecord<byte[], byte[]> record : records) {
                currentEvent.updateProgress(record);

                Record current = newRecord(record, options);
                if (searchFilter(options, current)) {
                    list.add(current);
                    matchesCount.getAndIncrement();

                    log.trace(
                        "Record [topic: {}] [partition: {}] [offset: {}] [key: {}]",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        record.key()
                    );
                }
            }

            currentEvent.records = list;

            if (list.size() > 0) {
                emitter.onNext(currentEvent.progress(options));
            } else if (currentEvent.emptyPoll >= 1 || matchesCount.get() >= options.getSize()) {
                emitter.onNext(currentEvent.end());
                currentEvent.emptyPoll = 666;
            } else {
                emitter.onNext(currentEvent.progress(options));
            }

            return currentEvent;
        });
    }

    private boolean searchFilter(Options options, Record record) {
        if (record.getKeyAsString() != null && record.getKeyAsString().toLowerCase().contains(options.getSearch().toLowerCase())) {
            return true;
        }

        return record.getValueAsString() != null && record.getValueAsString().toLowerCase()
            .contains(options.getSearch().toLowerCase());
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    public static class SearchEvent {
        private Map<Integer, Offset> offsets = new HashMap<>();
        private List<Record> records = new ArrayList<>();
        private String after;
        private double percent;
        private double emptyPoll = 0;

        private SearchEvent(SearchEvent event) {
            this.offsets = event.offsets;
        }

        private SearchEvent(Topic topic) {
            topic.getPartitions()
                .forEach(partition -> {
                    offsets.put(partition.getId(), new Offset(partition.getFirstOffset(), partition.getFirstOffset(), partition.getLastOffset()));
                });
        }

        public Event<SearchEvent> end() {
            this.percent = 100;

            return Event.of(this).name("searchEnd");
        }

        public Event<SearchEvent> progress(Options options) {
            long total = 0;
            long current = 0;

            for (Map.Entry<Integer, Offset> item : this.offsets.entrySet()) {
                total += item.getValue().end - item.getValue().begin;
                current += item.getValue().current - item.getValue().begin;
            }

            this.percent = (double) (current * 100) / total;
            this.after = options.pagination(offsets);

            return Event.of(this).name("searchBody");
        }


        private void updateProgress(ConsumerRecord<byte[], byte[]> record) {
            Offset offset = this.offsets.get(record.partition());
            offset.current = record.offset();
        }

        @AllArgsConstructor
        @Setter
        public static class Offset {
            @JsonProperty("begin")
            private final long begin;

            @JsonProperty("current")
            private long current;

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
        private int size;
        private Map<Integer, Long> after = new HashMap<>();
        private Sort sort;
        private Integer partition;
        private Long timestamp;
        private String search;
        private String schemaKey;
        private String schemaValue;

        public Options(Environment environment, String clusterId, String topic) {
            this.sort = environment.getProperty("kafkahq.topic-data.sort", Sort.class, Sort.OLDEST);
            this.size = environment.getProperty("kafkahq.topic-data.size", Integer.class, 50);

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

        public String pagination(Map<Integer, SearchEvent.Offset> offsets) {
            Map<Integer, Long> next = new HashMap<>(this.after);

            for (Map.Entry<Integer, SearchEvent.Offset> offset : offsets.entrySet()) {
                if (this.sort == Sort.OLDEST && (!next.containsKey(offset.getKey()) || next.get(offset.getKey()) < offset.getValue().current)) {
                    next.put(offset.getKey(), offset.getValue().current);
                } else if (this.sort == Sort.NEWEST && (!next.containsKey(offset.getKey()) || next.get(offset.getKey()) > offset.getValue().current)) {
                    next.put(offset.getKey(), offset.getValue().current);
                }
            }

            return paginationLink(next);
        }

        public String pagination(List<Record> records) {
            Map<Integer, Long> next = new HashMap<>(this.after);
            for (Record record : records) {
                if (this.sort == Sort.OLDEST && (!next.containsKey(record.getPartition()) || next.get(record.getPartition()) < record.getOffset())) {
                    next.put(record.getPartition(), record.getOffset());
                } else if (this.sort == Sort.NEWEST && (!next.containsKey(record.getPartition()) || next.get(record.getPartition()) > record.getOffset())) {
                    next.put(record.getPartition(), record.getOffset());
                }
            }

            return paginationLink(next);
        }

        private String paginationLink(Map<Integer, Long> next) {
            ArrayList<String> segment = new ArrayList<>();

            for (Map.Entry<Integer, Long> offset : next.entrySet()) {
                segment.add(offset.getKey() + "-" + offset.getValue());
            }

            if (next.size() > 0) {
                return String.join("_", segment);
            }

            return null;
        }

        public URIBuilder after(List<Record> records, URIBuilder uri) {
            if (records.size() == 0) {
                return URIBuilder.empty();
            }

            return uri.addParameter("after", pagination(records));
        }

        public URIBuilder before(List<Record> records, URIBuilder uri) {
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
        private final KafkaConsumer<byte[], byte[]> consumer;
    }
}
