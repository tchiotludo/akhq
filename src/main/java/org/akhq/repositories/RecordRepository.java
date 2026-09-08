package org.akhq.repositories;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.sse.Event;
import java.time.Duration;
import java.util.stream.StreamSupport;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.SchemaRegistryType;
import org.akhq.controllers.TopicController;
import org.akhq.models.KeyValue;
import org.akhq.models.Partition;
import org.akhq.models.Record;
import org.akhq.models.Topic;
import org.akhq.models.Schema;
import org.akhq.models.audit.RecordAuditEvent;
import org.akhq.modules.AuditModule;
import org.akhq.modules.KafkaModule;
import org.akhq.modules.schemaregistry.SchemaSerializer;
import org.akhq.modules.schemaregistry.RecordWithSchemaSerializerFactory;
import org.akhq.search.TopicSearchPlanner;
import org.akhq.search.RecordSearchFilter;
import org.akhq.utils.AvroToJsonSerializer;
import org.akhq.utils.Debug;
import org.akhq.utils.Masker;
import org.apache.kafka.clients.admin.DeletedRecords;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.codehaus.httpcache4j.uri.URIBuilder;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@Slf4j
public class RecordRepository extends AbstractRepository {
    @Inject
    private KafkaModule kafkaModule;

    @Inject
    private AuditModule auditModule;

    @Inject
    private ConfigRepository configRepository;

    @Inject
    private AvroToJsonSerializer avroToJsonSerializer;

    @Inject
    private TopicRepository topicRepository;

    @Inject
    private SchemaRegistryRepository schemaRegistryRepository;

    @Inject
    private RecordWithSchemaSerializerFactory serializerFactory;

    @Inject
    private CustomDeserializerRepository customDeserializerRepository;

    @Inject
    private AvroWireFormatConverter avroWireFormatConverter;

    @Inject
    private Masker masker;

    @Inject
    private TopicSearchPlanner topicSearchPlanner;

    @Inject
    private RecordSearchFilter recordSearchFilter;

    @Value("${akhq.topic-data.poll-timeout:1000}")
    protected int pollTimeout;

    @Value("${akhq.topic-data.kafka-max-message-length:2147483647}")
    protected int maxKafkaMessageLength;

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
        List<Record> list = new ArrayList<>();

        Properties properties = new Properties() {{
            put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, options.size);
        }};

        try (KafkaConsumer<byte[], byte[]> consumer = this.kafkaModule.getConsumer(options.clusterId, properties)) {
            TopicSearchPlanner.PartitionRangePlan rangePlan = topicSearchPlanner.resolvePartitionRangePlan(topic, options, consumer, false);
            if (rangePlan.isEmpty()) {
                return list;
            }

            consumer.assign(rangePlan.ranges().keySet());
            rangePlan.ranges().values().forEach(range -> consumer.seek(range.topicPartition(), range.begin()));

            ConsumerRecords<byte[], byte[]> records = this.poll(consumer);
            ConsumerRecords<byte[], byte[]> filteredRecords = filterRecordsByPartitionRange(records, rangePlan.getRangeEnds());

            for (ConsumerRecord<byte[], byte[]> record : filteredRecords) {
                Record current = newRecord(record, options, topic);
                boolean matched = recordSearchFilter.matchFilters(options, current);
                if (matched) {
                    filterMessageLength(current);
                    list.add(current);
                }
            }
        }

        return list.stream()
            .sorted(Comparator.comparing(Record::getTimestamp))
            .limit(options.size)
            .toList();
    }

    private List<Record> consumeNewest(Topic topic, Options options) {
        Properties properties = new Properties() {{
            put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, options.size);
        }};

        try (KafkaConsumer<byte[], byte[]> consumer = this.kafkaModule.getConsumer(options.clusterId, properties)) {
            TopicSearchPlanner.PartitionRangePlan rangePlan = topicSearchPlanner.resolvePartitionRangePlan(topic, options, consumer, true);
            if (rangePlan.isEmpty()) {
                return Collections.emptyList();
            }

            consumer.assign(rangePlan.ranges().keySet());
            rangePlan.ranges().values().forEach(range -> consumer.seek(range.topicPartition(), range.begin()));

            rangePlan.ranges().values().forEach(range ->
                log.trace(
                    "Consume Newest [topic: {}] [partition: {}] [start: {}] [end: {}]",
                    range.topicPartition().topic(),
                    range.topicPartition().partition(),
                    range.begin(),
                    range.end()
                )
            );

            List<Record> list = new ArrayList<>();
            int emptyPoll = 0;
            do {
                ConsumerRecords<byte[], byte[]> records = filterRecordsByPartitionRange(
                    this.poll(consumer),
                    rangePlan.getRangeEnds()
                );

                if (records.isEmpty()) {
                    emptyPoll++;
                } else {
                    emptyPoll = 0;
                }

                for (ConsumerRecord<byte[], byte[]> record : records) {
                    Record current = newRecord(record, options, topic);
                    boolean matched = recordSearchFilter.matchFilters(options, current);
                    if (matched) {
                        filterMessageLength(current);
                        list.add(current);
                    }
                }
            } while (emptyPoll < 1 && list.size() < options.size);

            List<Record> result = new ArrayList<>(list);
            result.sort(Comparator.comparing(Record::getTimestamp).reversed());
            return result.stream().limit(options.size).collect(Collectors.toList());
        }
    }

    public Optional<Record> consumeSingleRecord(String clusterId, Topic topic, Options options) throws ExecutionException, InterruptedException {
        return Debug.call(() -> {
            if (options.getPartition() == null) {
                return Optional.empty();
            }

            Properties properties = new Properties() {{
                put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
            }};

            try (KafkaConsumer<byte[], byte[]> consumer = kafkaModule.getConsumer(clusterId, properties)) {
                TopicPartition targetPartition = new TopicPartition(topic.getName(), options.getPartition());
                long startOffset = Math.max(0L, options.after.getOrDefault(options.getPartition(), -1L) + 1);
                consumer.assign(Collections.singleton(targetPartition));
                consumer.seek(targetPartition, startOffset);

                ConsumerRecords<byte[], byte[]> records = this.poll(consumer);
                if (records.isEmpty()) {
                    return Optional.empty();
                }

                Optional<ConsumerRecord<byte[], byte[]>> record = records.records(targetPartition).stream()
                    .filter(r -> r.offset() >= startOffset)
                    .findFirst();

                return record.map(r -> newRecord(r, options, topic));
            }
        }, "Consume with options {}", Collections.singletonList(options.toString()));
    }

    public Flux<Event<SearchEvent>> search(Topic topic, Options options) throws ExecutionException, InterruptedException {
        AtomicInteger matchesCount = new AtomicInteger();
        AtomicInteger iteration = new AtomicInteger();

        return Flux.generate(() -> {
            KafkaConsumer<byte[], byte[]> consumer = this.kafkaModule.getConsumer(options.clusterId);
            TopicSearchPlanner.PartitionRangePlan rangePlan = topicSearchPlanner.resolvePartitionRangePlan(topic, options, consumer, false);

            if (rangePlan.isEmpty()) {
                return new SearchState(consumer, null, Collections.emptyMap());
            }

            consumer.assign(rangePlan.ranges().keySet());
            rangePlan.ranges().values().forEach(range -> consumer.seek(range.topicPartition(), range.begin()));

            rangePlan.ranges().values().forEach(range ->
                log.trace(
                    "Search [topic: {}] [partition: {}] [start: {}] [end: {}]",
                    range.topicPartition().topic(),
                    range.topicPartition().partition(),
                    range.begin(),
                    range.end()
                )
            );

            return new SearchState(consumer, new SearchEvent(topic), rangePlan.getRangeEnds());
        }, (searchState, emitter) -> {
            SearchEvent searchEvent = searchState.getSearchEvent();
            KafkaConsumer<byte[], byte[]> consumer = searchState.getConsumer();

            // end
            if (searchEvent == null || searchEvent.emptyPoll >= 1) {
                emitter.next(new SearchEvent(topic).end(searchEvent != null ? searchEvent.after: null));
                emitter.complete();

                return new SearchState(consumer, searchEvent, searchState.getRangeEnds());
            }

            SearchEvent currentEvent = new SearchEvent(searchEvent);
            int currentIteration = iteration.incrementAndGet();

            ConsumerRecords<byte[], byte[]> records = this.poll(consumer);
            ConsumerRecords<byte[], byte[]> filteredRecords = filterRecordsByPartitionRange(records, searchState.getRangeEnds());

            if (filteredRecords.isEmpty()) {
                currentEvent.emptyPoll = 1;
            } else {
                currentEvent.emptyPoll = 0;
            }

            log.trace(
                "Search poll iteration #{} [topic: {}] [ranges: {}] [pollRecords: {}] [filteredRecords: {}]",
                currentIteration,
                topic.getName(),
                currentEvent.getOffsets().entrySet().stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue().begin + "-" + entry.getValue().end)
                    .collect(Collectors.toList()),
                records.count(),
                filteredRecords.count()
            );

            Comparator<Record> comparator = Comparator.comparing(Record::getTimestamp);

            List<Record> sortedRecords = StreamSupport.stream(filteredRecords.spliterator(), false)
                .map(record -> newRecord(record, options, topic))
                .sorted(Options.Sort.NEWEST.equals(options.sort) ? comparator.reversed() : comparator)
                .toList();

            List<Record> list = new ArrayList<>();

            for (Record record : sortedRecords) {
                if (matchesCount.get() >= options.size) {
                    break;
                }

                currentEvent.updateProgress(record);

                if (recordSearchFilter.matchFilters(options, record)) {
                    list.add(record);
                    matchesCount.getAndIncrement();

                    SearchEvent.Offset partitionOffset = currentEvent.getOffsets().get(record.getPartition());
                    log.trace(
                        "Search matched record [topic: {}] [partition: {}] [offset: {}] [key: {}] [iteration: {}] [bounds: {}]",
                        record.getTopic(),
                        record.getPartition(),
                        record.getOffset(),
                        record.getKey(),
                        currentIteration,
                        partitionOffset == null ? "n/a" : partitionOffset.begin + "-" + partitionOffset.end
                    );
                }
            }

            currentEvent.records = list;

            // No more records, poll was empty: stop here
            if (currentEvent.emptyPoll == 1) {
                emitter.next(currentEvent.end(searchEvent.getAfter()));
            }
            // More records than expected, send the records and then stop
            else if (matchesCount.get() >= options.getSize()) {
                currentEvent.emptyPoll = 666;
                emitter.next(currentEvent.progress(options));
            }
            // Continue to search
            else {
                emitter.next(currentEvent.progress(options));
            }

            return new SearchState(consumer, currentEvent, searchState.getRangeEnds());
        }, searchState -> searchState.getConsumer().close());
    }

    public Map<String, Record> getLastRecord(String clusterId, List<String> topicsName) throws ExecutionException, InterruptedException {
        Map<String, Topic> topics = topicRepository.findByName(clusterId, topicsName).stream()
            .collect(Collectors.toMap(Topic::getName, Function.identity()));

        List<TopicPartition> topicPartitions = topics.values()
            .stream()
            .flatMap(topic -> topic.getPartitions().stream())
            .map(partition -> new TopicPartition(partition.getTopic(), partition.getId()))
            .collect(Collectors.toList());

        ConcurrentHashMap<String, Record> records = new ConcurrentHashMap<>();

        try (KafkaConsumer<byte[], byte[]> consumer = kafkaModule.getConsumer(clusterId)) {
            consumer.assign(topicPartitions);

            Map<TopicPartition, Long> beginOffsets = consumer.beginningOffsets(consumer.assignment());
            consumer
                .endOffsets(consumer.assignment())
                .forEach((topicPartition, offset) -> {
                    long beginOffset = beginOffsets.getOrDefault(topicPartition, 0L);
                    consumer.seek(topicPartition, Math.max(beginOffset, offset - 2));
                });

            this.poll(consumer)
                .forEach(record -> {
                    if (!records.containsKey(record.topic())) {
                        records.put(record.topic(), newRecord(record, clusterId, topics.get(record.topic())));
                    } else {
                        Record current = records.get(record.topic());
                        if (current.getTimestamp().toInstant().toEpochMilli() < record.timestamp()) {
                            records.put(record.topic(), newRecord(record, clusterId, topics.get(record.topic())));
                        }
                    }

                });
        }

        return records;
    }

    public List<RecordMetadata> produce(
        String clusterId,
        String topic,
        Optional<String> value,
        List<KeyValue<String, String>> headers,
        Optional<String> key,
        Optional<Integer> partition,
        Optional<Long> timestamp,
        Optional<String> keySchema,
        Optional<String> valueSchema,
        Boolean multiMessage,
        Optional<String> keyValueSeparator) throws ExecutionException, InterruptedException, RestClientException, IOException {

        List<RecordMetadata> produceResults = new ArrayList<>();

        // Distinguish between single record produce, and multiple messages
        if (Boolean.TRUE.equals(multiMessage) && value.isPresent()) {
            // Split key-value pairs and produce them
            for (KeyValue<String, String> kvPair : splitMultiMessage(value.get(), keyValueSeparator.orElseThrow())) {
                produceResults.add(produce(clusterId, topic, Optional.of(kvPair.getValue()), headers, Optional.of(kvPair.getKey()),
                    partition, timestamp, keySchema, valueSchema));
            }
        } else {
            produceResults.add(
                produce(clusterId, topic, value, headers, key, partition, timestamp, keySchema, valueSchema));
        }
        auditModule.save(RecordAuditEvent.produce(clusterId, topic, produceResults.size()));
        return produceResults;
    }

    private RecordMetadata produce(
        String clusterId,
        String topic, byte[] value,
        List<KeyValue<String, String>> headers,
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
                headers == null ? Collections.emptyList() : headers
                    .stream()
                    .filter(entry -> StringUtils.isNotEmpty(entry.getKey()))
                    .map(entry -> new RecordHeader(
                        entry.getKey(),
                        entry.getValue() == null ? null : entry.getValue().getBytes()
                    ))
                    .collect(Collectors.toList())
            ))
            .get();
    }

    /**
     * Splits a multi-message into a list of key-value pairs.
     * @param value The multi-message string submitted by the {@link TopicController}
     * @param keyValueSeparator The character(s) separating each key from their corresponding value
     * @return A list of {@link KeyValue}, holding the split pairs
     */
    private List<KeyValue<String, String>> splitMultiMessage(String value, String keyValueSeparator) {
        return List.of(value.split("\r\n|\r|\n")).stream().map(v -> splitKeyValue(v, keyValueSeparator))
                .collect(Collectors.toList());
    }

    private KeyValue<String, String> splitKeyValue(String keyValueStr, String keyValueSeparator) {
        String[] keyValue = null;
        keyValue = keyValueStr.split(keyValueSeparator, 2);
        return new KeyValue<>(keyValue[0].trim(),keyValue[1]);
    }

    public void emptyTopic(String clusterId, String topicName) throws ExecutionException, InterruptedException {
        Map<TopicPartition, RecordsToDelete> recordsToDelete = new HashMap<>();
        var topic = topicRepository.findByName(clusterId, topicName);
        topic.getPartitions().forEach(partition -> {
            recordsToDelete.put(new TopicPartition(partition.getTopic(), partition.getId()),
                    RecordsToDelete.beforeOffset(partition.getLastOffset()));
        });
        deleteRecords(clusterId, recordsToDelete);
        auditModule.save(RecordAuditEvent.empty(clusterId, topicName));
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
        auditModule.save(RecordAuditEvent.empty(clusterId, topicName));
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
        Optional<String> value,
        List<KeyValue<String, String>> headers,
        Optional<String> key,
        Optional<Integer> partition,
        Optional<Long> timestamp,
        Optional<String> keySchema,
        Optional<String> valueSchema
    ) throws ExecutionException, InterruptedException, RestClientException, IOException {
        byte[] keyAsBytes = null;
        byte[] valueAsBytes;

        if (key.isPresent()) {
            if (keySchema.isPresent() && StringUtils.isNotEmpty(keySchema.get())) {
                Schema schema = schemaRegistryRepository.getLatestVersion(clusterId, keySchema.get());
                SchemaSerializer keySerializer = serializerFactory.createSerializer(clusterId, schema.getId());
                keyAsBytes = keySerializer.serialize(key.get());
            } else {
                keyAsBytes = key.filter(Predicate.not(String::isEmpty)).map(String::getBytes).orElse(null);
            }
        } else {
            try {
                if (Topic.isCompacted(configRepository.findByTopic(clusterId, value.isEmpty() ? null : value.get()))) {
                    throw new IllegalArgumentException("Key missing for produce onto compacted topic");
                }
            } catch (ExecutionException ex) {
                log.debug("Failed to determine if {} topic {} is compacted", clusterId, topic, ex);
            }
        }

        if (value.isPresent() && valueSchema.isPresent() && StringUtils.isNotEmpty(valueSchema.get())) {
            Schema schema = schemaRegistryRepository.getLatestVersion(clusterId, valueSchema.get());
            SchemaSerializer valueSerializer = serializerFactory.createSerializer(clusterId, schema.getId());
            valueAsBytes = valueSerializer.serialize(value.get());
        } else {
            valueAsBytes = value.filter(Predicate.not(String::isEmpty)).map(String::getBytes).orElse(null);
        }

        return produce(clusterId, topic, valueAsBytes, headers, keyAsBytes, partition, timestamp);
    }

    public RecordMetadata delete(String clusterId, String topic, Integer partition, byte[] key) throws ExecutionException, InterruptedException {
        RecordMetadata recordMetadata = kafkaModule.getProducer(clusterId).send(new ProducerRecord<>(
            topic,
            partition,
            key,
            null
        )).get();
        auditModule.save(RecordAuditEvent.delete(clusterId, topic, partition));
        return recordMetadata;
    }

    public Flux<Event<TailEvent>> tail(String clusterId, TailOptions options) {
        return Flux.generate(() -> {
            KafkaConsumer<byte[], byte[]> consumer = this.kafkaModule.getConsumer(options.clusterId);

            Map<String, Topic> topics = topicRepository.findByName(clusterId, options.topics).stream()
                    .collect(Collectors.toMap(Topic::getName, Function.identity()));

            consumer
                .assign(topics.values()
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

            return new TailState(consumer, new TailEvent(), topics);
        }, (state, emitter) -> {
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

                Record current = newRecord(record, options, state.getTopics().get(record.topic()));
                if (recordSearchFilter.matchFilters(options, current)) {
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
            emitter.next(Event.of(tailEvent).name("tailBody"));

            state.tailEvent = tailEvent;
            return state;
        }, tailState -> tailState.getConsumer().close());
    }

    public CopyResult copy(Topic fromTopic, String toClusterId, Topic toTopic, List<TopicController.OffsetCopy> offsets, RecordRepository.Options options) {
        Properties properties = new Properties() {{
            put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        }};

        try (KafkaConsumer<byte[], byte[]> consumer = this.kafkaModule.getConsumer(options.clusterId, properties)) {
            Map<TopicPartition, Long> partitions = topicSearchPlanner.resolvePartitionRangePlan(fromTopic, options, consumer, false)
                .ranges()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().begin()));

            Map<TopicPartition, Long> filteredPartitions = partitions.entrySet().stream()
                .filter(topicPartitionLongEntry -> offsets.stream()
                    .anyMatch(offsetCopy -> offsetCopy.getPartition() == topicPartitionLongEntry.getKey().partition()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            int counter = 0;

            if (!filteredPartitions.isEmpty()) {
                consumer.assign(filteredPartitions.keySet());
                filteredPartitions.forEach(consumer::seek);

                if (log.isTraceEnabled()) {
                    filteredPartitions.forEach((topicPartition, first) ->
                        log.trace(
                            "Consume [topic: {}] [partition: {}] [start: {}]",
                            topicPartition.topic(),
                            topicPartition.partition(),
                            first
                        )
                    );
                }

                Map<Partition, Long> partitionsLastOffsetMap = fromTopic.getPartitions()
                    .stream()
                    .collect(Collectors.toMap(Function.identity(), Partition::getLastOffset));

                boolean samePartition = toTopic.getPartitions().size() == fromTopic.getPartitions().size();

                KafkaProducer<byte[], byte[]> producer = kafkaModule.getProducer(toClusterId);
                ConsumerRecords<byte[], byte[]> records;
                do {
                    records = this.pollAndFilter(consumer, partitionsLastOffsetMap);

                    for (ConsumerRecord<byte[], byte[]> record : records) {
                        System.out.println(record.offset() + "-" + record.partition());

                        counter++;
                        producer.send(new ProducerRecord<>(
                            toTopic.getName(),
                            samePartition ? record.partition() : null,
                            record.timestamp(),
                            record.key(),
                            record.value(),
                            record.headers()
                        ));
                    }

                } while (!records.isEmpty());

                producer.flush();
            }

            return new CopyResult(counter);
        }
    }

    public List<TimeOffset> getOffsetForTime(String clusterId, List<org.akhq.models.TopicPartition> partitions, Long timestamp) throws ExecutionException, InterruptedException {
        return Debug.call(() -> {
            Map<TopicPartition, Long> map = new HashMap<>();

            try (KafkaConsumer<byte[], byte[]> consumer = this.kafkaModule.getConsumer(clusterId)) {
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
            }
        }, "Offsets for " + partitions + " Timestamp " + timestamp, null);
    }

    private ConsumerRecords<byte[], byte[]> filterRecordsByPartitionRange(
        ConsumerRecords<byte[], byte[]> records,
        Map<TopicPartition, Long> rangeEnds
    ) {
        if (records.isEmpty() || rangeEnds.isEmpty()) {
            return records;
        }

        Map<TopicPartition, List<ConsumerRecord<byte[], byte[]>>> filtered = new HashMap<>();
        rangeEnds.forEach((topicPartition, endOffset) -> {
            List<ConsumerRecord<byte[], byte[]>> partitionRecords = records.records(topicPartition)
                .stream()
                .filter(record -> record.offset() < endOffset)
                .collect(Collectors.toList());

            if (!partitionRecords.isEmpty()) {
                filtered.put(topicPartition, partitionRecords);
            }
        });

        return filtered.isEmpty() ? new ConsumerRecords<>(Collections.emptyMap()) : new ConsumerRecords<>(filtered);
    }

    /**
     * Polls the records and filters them with a maximum offset
     *
     * @param consumer
     * @param partitionsLastOffsetMap key : partition, value : the maximum offset we want to reach
     * @return filtered records after polled. And an empty one if there are no records polled
     * or if every record has been filtered
     */
    private ConsumerRecords<byte[], byte[]> pollAndFilter(KafkaConsumer<byte[], byte[]> consumer, Map<Partition, Long> partitionsLastOffsetMap) {
        ConsumerRecords<byte[], byte[]> records = this.poll(consumer);
        return new ConsumerRecords<>(partitionsLastOffsetMap.entrySet()
            .stream()
            .map(entry ->
                {
                    // We filter records by partition
                    TopicPartition topicPartition = new TopicPartition(entry.getKey().getTopic(), entry.getKey().getId());
                    return Map.entry(topicPartition, records.records(topicPartition)
                        .stream()
                        .filter(consumerRecord -> consumerRecord.offset() < entry.getValue())
                        .collect(Collectors.toList()));
                }
            ).filter(entry -> !entry.getValue().isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
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
        return consumer.poll(Duration.ofMillis(this.pollTimeout));
    }


    private Record newRecord(ConsumerRecord<byte[], byte[]> record, String clusterId, Topic topic) {
        SchemaRegistryType schemaRegistryType = this.schemaRegistryRepository.getSchemaRegistryType(clusterId);
        SchemaRegistryClient client = this.kafkaModule.getRegistryClient(clusterId);
        return masker.maskRecord(new Record(
            client,
            record,
            this.schemaRegistryRepository.getSchemaRegistryType(clusterId),
            this.schemaRegistryRepository.getKafkaAvroDeserializer(clusterId),
            schemaRegistryType == SchemaRegistryType.CONFLUENT? this.schemaRegistryRepository.getKafkaJsonDeserializer(clusterId):null,
            schemaRegistryType == SchemaRegistryType.CONFLUENT? this.schemaRegistryRepository.getKafkaProtoDeserializer(clusterId):null,
            this.avroToJsonSerializer,
            this.customDeserializerRepository.getProtobufToJsonDeserializer(clusterId),
            this.customDeserializerRepository.getAvroToJsonDeserializer(clusterId),
            avroWireFormatConverter.convertValueToWireFormat(record, client,
                this.schemaRegistryRepository.getSchemaRegistryType(clusterId)),
            topic,
            schemaRegistryType == SchemaRegistryType.GLUE ? schemaRegistryRepository.getAwsGlueKafkaDeserializer(clusterId): null
        ));
    }

    private Record newRecord(ConsumerRecord<byte[], byte[]> record, BaseOptions options, Topic topic) {
        SchemaRegistryType schemaRegistryType = this.schemaRegistryRepository.getSchemaRegistryType(options.clusterId);
        SchemaRegistryClient client = this.kafkaModule.getRegistryClient(options.clusterId);
        return masker.maskRecord(new Record(
            client,
            record,
            schemaRegistryType,
            this.schemaRegistryRepository.getKafkaAvroDeserializer(options.clusterId),
            schemaRegistryType == SchemaRegistryType.CONFLUENT? this.schemaRegistryRepository.getKafkaJsonDeserializer(options.clusterId):null,
            schemaRegistryType == SchemaRegistryType.CONFLUENT? this.schemaRegistryRepository.getKafkaProtoDeserializer(options.clusterId):null,
            this.avroToJsonSerializer,
            this.customDeserializerRepository.getProtobufToJsonDeserializer(options.clusterId),
            this.customDeserializerRepository.getAvroToJsonDeserializer(options.clusterId),
            avroWireFormatConverter.convertValueToWireFormat(record, client,
                this.schemaRegistryRepository.getSchemaRegistryType(options.clusterId)),
            topic,
            schemaRegistryType == SchemaRegistryType.GLUE ? schemaRegistryRepository.getAwsGlueKafkaDeserializer(options.getClusterId()): null
        ));
    }

    public static Map<TopicPartition, PartitionRange> buildPartitionRanges(Map<TopicPartition, Long> starts, Map<TopicPartition, Long> ends) {
        return TopicSearchPlanner.buildPartitionRanges(starts, ends).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> new PartitionRange(entry.getKey(), entry.getValue().begin(), entry.getValue().end())));
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @AllArgsConstructor
    public static class PartitionRange {
        private final TopicPartition topicPartition;
        private final long begin;
        private final long end;
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

    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor
    @Getter
    public static class CopyResult {
        int records;
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @AllArgsConstructor
    public static class TailState {
        private final KafkaConsumer<byte[], byte[]> consumer;
        private TailEvent tailEvent;
        private Map<String, Topic> topics;
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
    @AllArgsConstructor
    public static class SearchState {
        private final KafkaConsumer<byte[], byte[]> consumer;
        private final SearchEvent searchEvent;
        private final Map<TopicPartition, Long> rangeEnds;
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

        public Event<SearchEvent> end(String after) {
            this.percent = 100;
            this.after = after;

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

        private void updateProgress(Record record) {
            Offset offset = this.offsets.get(record.getPartition());
            offset.current = record.getOffset();
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
    public static class Search {

        public enum SearchMatchType {
            EQUALS("E"),
            CONTAINS("C"),
            NOT_CONTAINS("N");

            private final String code;

            SearchMatchType(String code) {
                this.code = code;
            }

            public static SearchMatchType valueOfCode(String code) {
                for (SearchMatchType e : values()) {
                    if (e.code.equals(code)) {
                        return e;
                    }
                }
                return null;
            }
        }

        protected String text;
        protected SearchMatchType searchMatchType;

        public Search(String text) {
            this.setText(text);
            this.searchMatchType = SearchMatchType.CONTAINS;
        }

        public Search(String text, String searchMatchType) {
            this.setText(text);
            this.setSearchMatchType(searchMatchType);
        }

        public void setText(String text) {
            this.text = text;
        }

        public void setSearchMatchType(String type) {
            this.searchMatchType = SearchMatchType.valueOfCode(type);
        }
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @Setter
    abstract public static class BaseOptions {

        protected String clusterId;
        protected Search search;
        protected Search searchByKey;
        protected Search searchByValue;
        protected Search searchByHeaderKey;
        protected Search searchByHeaderValue;
        protected Search searchByKeySubject;
        protected Search searchByValueSubject;

        public BaseOptions() {
        }

        public void setSearchByKey(String searchByKey) {
           this.searchByKey = this.buildSearch(searchByKey);
        }

        public void setSearchByValue(String searchByValue) {
            this.searchByValue = this.buildSearch(searchByValue);
        }

        public void setSearchByHeaderKey(String searchByHeaderKey) {
            this.searchByHeaderKey = this.buildSearch(searchByHeaderKey);
        }

        public void setSearchByHeaderValue(String searchByHeaderValue) {
            this.searchByHeaderValue = this.buildSearch(searchByHeaderValue);
        }

        public void setSearchByKeySubject(String searchByKeySubject) {
            this.searchByKeySubject = this.buildSearch(searchByKeySubject);
        }

        public void setSearchByValueSubject(String searchByValueSchemaName) {
            this.searchByValueSubject = this.buildSearch(searchByValueSchemaName);
        }

        public void setSearch(String search) {
            this.search = new Search(search);
        }

        private Search buildSearch(String search) {
            int sepPos = search.lastIndexOf('_');
            if(sepPos > 0) {
                return new Search(search.substring(0, sepPos), search.substring(sepPos + 1));
            } else {
                return new Search(search);
            }
        }

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
        private Long endTimestamp;

        public Options(Environment environment, String clusterId, String topic) {
            this.sort = Sort.OLDEST;
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

    private void filterMessageLength(Record record) {
        if (maxKafkaMessageLength == Integer.MAX_VALUE || record.getValue() == null) {
            return;
        }

        int bytesLength = record.getValue().getBytes(StandardCharsets.UTF_8).length;
        if (bytesLength > maxKafkaMessageLength) {
            int substringChars = maxKafkaMessageLength / 1000;
            record.setValue(record.getValue().substring(0, substringChars));
            record.setTruncated(true);
        }
    }
}
