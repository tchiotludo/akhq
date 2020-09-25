package org.akhq.repositories;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.http.sse.Event;
import io.reactivex.Flowable;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.DeletedRecords;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.akhq.models.Partition;
import org.akhq.models.Record;
import org.akhq.models.Topic;
import org.akhq.modules.AvroSerializer;
import org.akhq.modules.KafkaModule;
import org.akhq.utils.Debug;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.codehaus.httpcache4j.uri.URIBuilder;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Slf4j
public class RecordRepository extends AbstractRepository {
    @Inject
    private KafkaModule kafkaModule;

    @Inject
    private TopicRepository topicRepository;

    @Inject
    private SchemaRegistryRepository schemaRegistryRepository;

    @Inject
    private AvroWireFormatConverter avroWireFormatConverter;

    @Value("${akhq.topic-data.poll-timeout:1000}")
    protected int pollTimeout;

    public List<Record> consume(String clusterId, Options options) throws ExecutionException, InterruptedException {
        return Debug.call(() -> {
            Topic topicsDetail = topicRepository.findByName(clusterId, options.topic);

            if (options.sort == Options.Sort.OLDEST) {
                return consumeOldest(topicsDetail, options);
            } else {
                return consumeNewest(topicsDetail, options);
            }
        }, "Consume with options {}", Collections.singletonList(options.toString()));
    }

    private List<Record> consumeOldest(Topic topic, Options options) {
        KafkaConsumer<byte[], byte[]> consumer = this.kafkaModule.getConsumer(options.clusterId);
        Map<TopicPartition, Long> partitions = getTopicPartitionForSortOldest(topic, options, consumer);
        List<Record> list = new ArrayList<>();

        if (partitions.size() > 0) {
            consumer.assign(partitions.keySet());
            partitions.forEach(consumer::seek);

            if (log.isTraceEnabled()) {
                partitions.forEach((topicPartition, first) ->
                    log.trace(
                        "Consume [topic: {}] [partition: {}] [start: {}]",
                        topicPartition.topic(),
                        topicPartition.partition(),
                        first
                    )
                );
            }

            ConsumerRecords<byte[], byte[]> records = this.poll(consumer);

            for (ConsumerRecord<byte[], byte[]> record : records) {
                Record current = newRecord(record, options);
                if (searchFilter(options, current)) {
                    list.add(current);
                }
            }
        }

        consumer.close();

        list.sort(Comparator.comparing(Record::getTimestamp));

        return list;
    }

    public List<TimeOffset> getOffsetForTime(String clusterId, List<org.akhq.models.TopicPartition> partitions, Long timestamp) throws ExecutionException, InterruptedException {
        return Debug.call(() -> {
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

        }, "Offsets for " + partitions + " Timestamp " + timestamp, null);
    }

    public Optional<Record> consumeSingleRecord(String clusterId, Topic topic, Options options) throws ExecutionException, InterruptedException {
        return Debug.call(() -> {
            Optional<Record> singleRecord = Optional.empty();
            KafkaConsumer<byte[], byte[]> consumer = kafkaModule.getConsumer(clusterId, new Properties() {{
                put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
            }});

            Map<TopicPartition, Long> partitions = getTopicPartitionForSortOldest(topic, options, consumer);
            consumer.assign(partitions.keySet());
            partitions.forEach(consumer::seek);

            ConsumerRecords<byte[], byte[]> records = this.poll(consumer);
            if(!records.isEmpty()) {
                singleRecord = Optional.of(newRecord(records.iterator().next(), options));
            }

            consumer.close();
            return singleRecord;

        }, "Consume with options {}", Collections.singletonList(options.toString()));
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @AllArgsConstructor
    public static class TimeOffset {
        private final String topic;
        private final int partition;
        private final long offset;
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

                if (log.isTraceEnabled()) {
                    log.trace(
                        "Consume Newest [topic: {}] [partition: {}] [start: {}]",
                        topicPartitionOffset.getTopicPartition().topic(),
                        topicPartitionOffset.getTopicPartition().partition(),
                        topicPartitionOffset.getBegin()
                    );
                }

                List<Record> list = new ArrayList<>();
                int emptyPoll = 0;

                do {
                    ConsumerRecords<byte[], byte[]> records;

                    records = this.poll(topicPartitionOffset.getConsumer());

                    if (records.isEmpty()) {
                        emptyPoll++;
                    } else {
                        if (log.isTraceEnabled()) {
                            log.trace(
                                "Empty pool [topic: {}] [partition: {}]",
                                topicPartitionOffset.getTopicPartition().topic(),
                                topicPartitionOffset.getTopicPartition().partition()
                            );
                        }
                        emptyPoll = 0;
                    }

                    for (ConsumerRecord<byte[], byte[]> record : records) {
                        if (record.offset() > topicPartitionOffset.getEnd()) {
                            emptyPoll = 2;
                            break;
                        }
                        Record current = newRecord(record, options);
                        if (searchFilter(options, current)) {
                            list.add(current);
                        }
                    }
                }
                while (emptyPoll < 1);

                Collections.reverse(list);

                topicPartitionOffset.getConsumer().close();

                return Stream.of(list);
            })
            .flatMap(List::stream)
            .sorted(Comparator.comparing(Record::getTimestamp).reversed())
            .collect(Collectors.toList());
    }

    private int pollSizePerPartition(Topic topic, Options options) {
        if (options.partition != null) {
            return options.size;
        } else {
            return (int) Math.ceil(options.size * 1.0 / topic.getPartitions().size());
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

    private Record newRecord(ConsumerRecord<byte[], byte[]> record, BaseOptions options) {
        return new Record(
            record,
            this.schemaRegistryRepository.getKafkaAvroDeserializer(options.clusterId),
            avroWireFormatConverter.convertValueToWireFormat(record, this.kafkaModule.getRegistryClient(options.clusterId))
        );
    }

    private RecordMetadata produce(
        String clusterId,
        String topic, byte[] value,
        Map<String, String> headers,
        byte[] key,
        Optional<Integer> partition,
        Optional<Long> timestamp
    ) throws ExecutionException, InterruptedException {
        return kafkaModule
            .getProducer(clusterId)
            .send(new ProducerRecord<>(
                topic,
                partition.orElse(null),
                timestamp.orElse(null),
                key,
                value,
                (headers == null ? ImmutableMap.<String, String>of() : headers)
                    .entrySet()
                    .stream()
                    .map(entry -> new RecordHeader(
                        entry.getKey(),
                        entry.getValue() == null ? null : entry.getValue().getBytes()
                    ))
                    .collect(Collectors.toList())
            ))
            .get();
    }

    public void emptyTopic(String clusterId, String topicName) throws ExecutionException, InterruptedException {
        Map<TopicPartition, RecordsToDelete> recordsToDelete = new HashMap<>();
        var topic = topicRepository.findByName(clusterId, topicName);
        topic.getPartitions().forEach(partition -> {
            recordsToDelete.put(new TopicPartition(partition.getTopic(), partition.getId()),
                    RecordsToDelete.beforeOffset(partition.getLastOffset()));
        });
        deleteRecords(clusterId, recordsToDelete);
    }

    public void emptyTopicByTimestamp(String clusterId,
                                      String topicName,
                                      Long timestamp) throws ExecutionException, InterruptedException {
        Map<TopicPartition, Long> timestamps = new HashMap<>();
        Map<TopicPartition, RecordsToDelete> recordsToDelete = new HashMap<>();
        var topic = topicRepository.findByName(clusterId, topicName);
        topic.getPartitions().forEach(partition -> {
            timestamps.put(new TopicPartition(partition.getTopic(), partition.getId()),
                            timestamp);
        });
        Map<TopicPartition, OffsetAndTimestamp> offsets = kafkaModule.getConsumer(clusterId).offsetsForTimes(timestamps);

        offsets.forEach((topicPartition, offsetAndTimestamp) -> {
            recordsToDelete.put(topicPartition, RecordsToDelete.beforeOffset(offsetAndTimestamp.offset()));
        });
        deleteRecords(clusterId, recordsToDelete);

    }

    private void deleteRecords(String clusterId, Map<TopicPartition, RecordsToDelete> recordsToDelete) throws InterruptedException, ExecutionException {
        var deleted = kafkaModule.getAdminClient(clusterId).deleteRecords(recordsToDelete).lowWatermarks();
        for (Map.Entry<TopicPartition, KafkaFuture<DeletedRecords>> entry : deleted.entrySet()){
            log.debug(entry.getKey().topic() + " " + entry.getKey().partition() + " " + entry.getValue().get().lowWatermark());
        }
    }

    public RecordMetadata produce(
        String clusterId,
        String topic,
        String value,
        Map<String, String> headers,
        Optional<String> key,
        Optional<Integer> partition,
        Optional<Long> timestamp,
        Optional<Integer> keySchemaId,
        Optional<Integer> valueSchemaId
    ) throws ExecutionException, InterruptedException {
        AvroSerializer avroSerializer = this.schemaRegistryRepository.getAvroSerializer(clusterId);
        byte[] keyAsBytes = null;
        byte[] valueAsBytes;

        if (key.isPresent()) {
            if (keySchemaId.isPresent()) {
                keyAsBytes = avroSerializer.toAvro(key.get(), keySchemaId.get());
            } else {
                keyAsBytes = key.get().getBytes();
            }
        }

        if (value != null && valueSchemaId.isPresent()) {
            valueAsBytes = avroSerializer.toAvro(value, valueSchemaId.get());
        } else {
            valueAsBytes = value != null ? value.getBytes() : null;
        }

        return produce(clusterId, topic, valueAsBytes, headers, keyAsBytes, partition, timestamp);
    }

    public RecordMetadata delete(String clusterId, String topic, Integer partition, byte[] key) throws ExecutionException, InterruptedException {
        return kafkaModule.getProducer(clusterId).send(new ProducerRecord<>(
            topic,
            partition,
            key,
            null
        )).get();
    }

    public Flowable<Event<SearchEvent>> search(String clusterId, Options options) throws ExecutionException, InterruptedException {
        KafkaConsumer<byte[], byte[]> consumer = this.kafkaModule.getConsumer(options.clusterId);
        Topic topic = topicRepository.findByName(clusterId, options.topic);
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

            if (currentEvent.emptyPoll >= 1) {
                currentEvent.emptyPoll = 666;
                emitter.onNext(currentEvent.end());
            } else if (matchesCount.get() >= options.getSize()) {
                currentEvent.emptyPoll = 666;
                emitter.onNext(currentEvent.progress(options));
            } else {
                emitter.onNext(currentEvent.progress(options));
            }

            return currentEvent;
        });
    }

    private boolean searchFilter(BaseOptions options, Record record) {
        if (options.getSearch() == null) {
            return true;
        }

        if (record.getKey() != null && containsAll(options.getSearch(), record.getKey())) {
            return true;
        }

        return record.getValue() != null && containsAll(options.getSearch(), record.getValue());
    }

    private static boolean containsAll(String search, String in) {
        String[] split = search.toLowerCase().split("\\s");
        in = in.toLowerCase();

        for (String k : split) {
            if (!in.contains(k)) {
                return false;
            }
        }

        return true;
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    public static class SearchEvent {
        private Map<Integer, Offset> offsets = new HashMap<>();
        private List<Record> records = new ArrayList<>();
        private String after;
        private double percent;
        private int emptyPoll = 0;

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

    public Flowable<Event<TailEvent>> tail(String clusterId, TailOptions options) {
        return Flowable.generate(() -> {
            KafkaConsumer<byte[], byte[]> consumer = this.kafkaModule.getConsumer(options.clusterId);

            List<Topic> topics = topicRepository.findByName(clusterId, options.topics);

            consumer
                .assign(topics
                    .stream()
                    .flatMap(topic -> topic.getPartitions()
                        .stream()
                        .map(partition -> new TopicPartition(topic.getName(), partition.getId()))
                    )
                    .collect(Collectors.toList())
                );

            if (options.getAfter() != null) {
                options
                    .getAfter()
                    .forEach(s -> {
                        String[] split = s.split(",");
                        consumer.seek(
                            new TopicPartition(split[0], Integer.parseInt(split[1])),
                            Long.parseLong(split[2])
                        );
                    });
            }

            return new TailState(consumer, new TailEvent());
        }, (state, subscriber) -> {
            ConsumerRecords<byte[], byte[]> records = this.poll(state.getConsumer());
            TailEvent tailEvent = state.getTailEvent();

            List<Record> list = new ArrayList<>();

            for (ConsumerRecord<byte[], byte[]> record : records) {

                tailEvent.offsets.put(
                    ImmutableMap.of(
                        record.topic(),
                        record.partition()
                    ),
                    record.offset()
                );

                Record current = newRecord(record, options);
                if (searchFilter(options, current)) {
                    list.add(current);
                    log.trace(
                        "Record [topic: {}] [partition: {}] [offset: {}] [key: {}]",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        record.key()
                    );
                }
            }

            tailEvent.records = list;
            subscriber.onNext(Event.of(tailEvent).name("tailBody"));

            state.tailEvent = tailEvent;
            return state;
        });
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @AllArgsConstructor
    public static class TailState {
        private final KafkaConsumer<byte[], byte[]> consumer;
        private TailEvent tailEvent;
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    public static class TailEvent {
        private List<Record> records = new ArrayList<>();
        private final Map<Map<String, Integer>, Long> offsets = new HashMap<>();
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @Setter
    abstract public static class BaseOptions {
        protected String clusterId;
        protected String search;
    }

    @ToString
    @EqualsAndHashCode(callSuper = true)
    @Getter
    @Setter
    public static class Options extends BaseOptions {
        public enum Sort {
            OLDEST,
            NEWEST,
        }
        private String topic;
        private int size;
        private Map<Integer, Long> after = new HashMap<>();
        private Sort sort;
        private Integer partition;
        private Long timestamp;

        public Options(Environment environment, String clusterId, String topic) {
            this.sort = environment.getProperty("akhq.topic-data.sort", Sort.class, Sort.OLDEST);
            //noinspection ConstantConditions
            this.size = environment.getProperty("akhq.topic-data.size", Integer.class, 50);

            this.clusterId = clusterId;
            this.topic = topic;
        }

        public void setAfter(String after) {
            this.after.clear();

            //noinspection UnstableApiUsage
            Splitter.on('_')
                .withKeyValueSeparator('-')
                .split(after)
                .forEach((key, value) -> this.after.put(Integer.valueOf(key), Long.valueOf(value)));
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

    @ToString
    @EqualsAndHashCode(callSuper = true)
    @Getter
    @Setter
    public static class TailOptions extends BaseOptions {
        private List<String> topics;
        protected List<String> after;

        public TailOptions(String clusterId, List<String> topics) {
            this.clusterId = clusterId;
            this.topics = topics;
        }
    }

    @Data
    @Builder
    private static class OffsetBound {
        @With
        private final TopicPartition topicPartition;
        private final long begin;
    }

    @Data
    @Builder
    private static class EndOffsetBound {
        @With
        private final TopicPartition topicPartition;
        private final long begin;
        private final long end;
        private final KafkaConsumer<byte[], byte[]> consumer;
    }
}
