package org.akhq.repositories;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.env.Environment;
import io.micronaut.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.configs.SchemaRegistryType;
import org.akhq.controllers.TopicController;
import org.akhq.models.Record;
import org.akhq.models.Schema;
import org.akhq.models.Topic;
import org.akhq.utils.Album;
import org.akhq.utils.ResourceTestUtil;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.json.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junitpioneer.jupiter.RetryingTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class RecordRepositoryTest extends AbstractTest {
    @Inject
    private RecordRepository repository;

    @Inject
    private TopicRepository topicRepository;

    @Inject
    private SchemaRegistryRepository schemaRegistryRepository;

    @Inject
    private Environment environment;

    @Inject
    private JsonMapper jsonMapper;

    @Test
    void oldestCursorKeepsDroppedCandidatesAndSkipsScannedPartitions() throws ExecutionException, InterruptedException {
        // Skewed page: p0 holds the 10 oldest matches, p1 also collected 10 matches but lost them all
        // to the global sort/limit, p2 was scanned to its end with no match, p3 was only partly scanned
        // (empty-poll guard) with no match, and p4 was not scanned at all.
        Map<TopicPartition, Long> scannedUpTo = Map.of(
            new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 0), 20L,
            new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 1), 20L,
            new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 2), 50L,
            new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 3), 30L,
            new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 4), 0L
        );
        Map<TopicPartition, Integer> collected = Map.of(
            new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 0), 10,
            new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 1), 10
        );
        Topic topic = topicRepository.findByName(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        List<Record> emitted = new ArrayList<>();
        for (long offset = 0; offset < 10; offset++) {
            emitted.add(record(topic, 0, offset));
        }

        Map<Integer, Long> next = RecordRepository.nextOldestCursor(Map.of(1, 4L), scannedUpTo, collected, emitted);

        // p0 resumes after its last emitted record, p1 keeps its previous cursor so its dropped
        // candidates are matched again, p2/p3 resume after their last scanned offset, p4 has no cursor.
        assertEquals(Map.of(0, 9L, 1, 4L, 2, 49L, 3, 29L), next);
    }

    private static Record record(Topic topic, int partition, long offset) {
        return new Record(
            new RecordMetadata(new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, partition), offset, 0, 0L, 0, 0),
            SchemaRegistryType.CONFLUENT,
            new byte[0],
            new byte[0],
            Collections.emptyList(),
            topic,
            null
        );
    }

    @Test
    void buildPartitionRangesSkipsEmptyWindows() {
        Map<TopicPartition, Long> starts = new LinkedHashMap<>();
        starts.put(new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 0), 10L);
        starts.put(new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 1), 20L);
        starts.put(new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 2), 30L);

        Map<TopicPartition, Long> ends = new LinkedHashMap<>();
        ends.put(new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 0), 10L);
        ends.put(new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 1), 40L);
        ends.put(new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 2), 35L);

        Map<TopicPartition, RecordRepository.PartitionRange> ranges = RecordRepository.buildPartitionRanges(starts, ends);

        assertEquals(2, ranges.size());
        assertEquals(20L, ranges.get(new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 1)).getBegin());
        assertEquals(40L, ranges.get(new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 1)).getEnd());
        assertEquals(30L, ranges.get(new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 2)).getBegin());
        assertEquals(35L, ranges.get(new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 2)).getEnd());
    }

    @Test
    void buildPartitionRangesKeepsTimestampBoundedRange() {
        Map<TopicPartition, Long> starts = new LinkedHashMap<>();
        starts.put(new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 0), 100L);
        starts.put(new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 1), 200L);

        Map<TopicPartition, Long> ends = new LinkedHashMap<>();
        ends.put(new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 0), 150L);
        ends.put(new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 1), 200L);

        Map<TopicPartition, RecordRepository.PartitionRange> ranges = RecordRepository.buildPartitionRanges(starts, ends);

        assertEquals(1, ranges.size());
        RecordRepository.PartitionRange range = ranges.get(new TopicPartition(KafkaTestCluster.TOPIC_RANDOM, 0));
        assertEquals(100L, range.getBegin());
        assertEquals(150L, range.getEnd());
    }

    @Test
    void consumeEmpty() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_EMPTY);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        assertEquals(0, consumeAll(options));
    }

    @Test
    void consumeOldest() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        assertEquals(300, consumeAll(options));
    }

    @Test
    void consumeOldestFirstPageSpansAllPartitions() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_INTERLEAVED);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        List<Record> firstPage = repository.consume(KafkaTestCluster.CLUSTER_ID, options);

        // The interleaved topic round-robins strictly increasing timestamps across the 3 partitions, so
        // the oldest page must merge candidates from every partition. A single-poll implementation
        // returns a full page from just one partition, which this assertion catches.
        Set<Integer> partitions = firstPage.stream()
            .map(Record::getPartition)
            .collect(Collectors.toSet());
        assertEquals(3, partitions.size(), "Oldest page should contain records from all 3 partitions");

        // The page must be the true globally-oldest 'size' records: keys key_0..key_(size-1).
        Set<String> expectedKeys = new HashSet<>();
        for (int i = 0; i < options.getSize(); i++) {
            expectedKeys.add("key_" + i);
        }
        Set<String> actualKeys = firstPage.stream().map(Record::getKey).collect(Collectors.toSet());
        assertEquals(expectedKeys, actualKeys, "Oldest page should be the globally-oldest records by timestamp");
    }

    @Test
    void consumeOldestMultiPagePaginationHasNoGapsOrDuplicates() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_INTERLEAVED);
        options.setSort(RecordRepository.Options.Sort.OLDEST);
        options.setSize(10);

        // Forces many small pages (18) across the 3-partition topic, so a wrong 'after' cursor
        // (skipping or re-reading an offset on any partition) would show up as a gap or duplicate.
        List<String> keys = paginateConsume(options);

        assertEquals(expectedInterleavedKeys(false), keys, "Oldest pagination must return every record exactly once, in ascending order");
    }

    @Test
    void consumeOldestNoMatchesTerminatesImmediately() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_INTERLEAVED);
        options.setSort(RecordRepository.Options.Sort.OLDEST);
        options.setSearchByKey("no_such_key_E");

        List<Record> firstPage = repository.consume(KafkaTestCluster.CLUSTER_ID, options);

        assertTrue(firstPage.isEmpty(), "A non-matching filter must return an empty page (no pagination cursor to follow)");
    }

    @Test
    void consumeNewest() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        options.setSort(RecordRepository.Options.Sort.NEWEST);

        assertEquals(300, consumeAll(options));
    }

    @Test
    void consumeNewestMultiPagePaginationHasNoGapsOrDuplicates() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_INTERLEAVED);
        options.setSort(RecordRepository.Options.Sort.NEWEST);
        options.setSize(10);

        List<String> keys = paginateConsume(options);

        assertEquals(expectedInterleavedKeys(true), keys, "Newest pagination must return every record exactly once, in descending order");
    }

    @Test
    void consumeNewestNoMatchesTerminatesImmediately() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_INTERLEAVED);
        options.setSort(RecordRepository.Options.Sort.NEWEST);
        options.setSearchByKey("no_such_key_E");

        List<Record> firstPage = repository.consume(KafkaTestCluster.CLUSTER_ID, options);

        assertTrue(firstPage.isEmpty(), "A non-matching filter must return an empty page (no pagination cursor to follow)");
    }

    @Test
    void consumeOldestPerPartition() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        options.setSort(RecordRepository.Options.Sort.OLDEST);
        options.setPartition(1);

        assertEquals(100, consumeAll(options));
    }

    @Test
    void consumeNewestPerPartition() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        options.setSort(RecordRepository.Options.Sort.NEWEST);
        options.setPartition(1);

        assertEquals(100, consumeAll(options));
    }

    @Test
    void consumeOldestCompacted() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        assertEquals(153, consumeAll(options));
    }

    @Test
    void consumeNewestCompacted() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED);
        options.setSort(RecordRepository.Options.Sort.NEWEST);

        assertEquals(153, consumeAll(options));
    }

    @Test
    void consumeOldestPerPartitionCompacted() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED);
        options.setSort(RecordRepository.Options.Sort.OLDEST);
        options.setPartition(0);

        assertEquals(51, consumeAll(options));
    }

    @Test
    void consumeNewestPerPartitionCompacted() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED);
        options.setSort(RecordRepository.Options.Sort.NEWEST);
        options.setPartition(0);

        assertEquals(51, consumeAll(options));
    }

    @RetryingTest(maxAttempts = 3, suspendForMs = 1000)
    void consumeAvro() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_STREAM_MAP);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        List<Record> records = consumeAllRecord(options);

        assertEquals(12, records.size());


        Optional<Record> avroRecord = records
            .stream()
            .filter(record -> record.getKey().equals("1"))
            .findFirst();

        avroRecord.orElseThrow(() -> new NoSuchElementException("Unable to find key 1"));
        avroRecord.ifPresent(record -> {
            assertThat(record.getValue(), containsString("\"breed\":\"ABYSSINIAN\""));
            assertThat(record.getValue(), containsString("\"name\":\"WaWa\""));
            assertThat(record.getValue(), containsString("\"id\":1"));
        });
    }

    @Test
    void emptyTopic() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_TOBE_EMPTIED);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        repository.emptyTopic(options.clusterId, options.getTopic());
        assertEquals(0, consumeAll(options));
    }

    @Disabled("Method not ready yet")
    @Test
    void emptyTopicByTimestamp() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_TOBE_EMPTIED);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        repository.emptyTopicByTimestamp(options.clusterId, options.getTopic(), System.currentTimeMillis());
        assertEquals(0, consumeAll(options));
    }

    private List<Record> consumeAllRecord(RecordRepository.Options options) throws ExecutionException, InterruptedException {
        boolean hasNext = true;

        List<Record> all = new ArrayList<>();

        do {
            List<Record> datas = repository.consume(KafkaTestCluster.CLUSTER_ID, options);
            all.addAll(datas);

            datas.forEach(record -> log.debug(
                "Records [Topic: {}] [Partition: {}] [Offset: {}] [Key: {}] [Value: {}]",
                record.getTopic(),
                record.getPartition(),
                record.getOffset(),
                record.getKey(),
                record.getValue()
            ));
            log.info("Consume {} records", datas.size());

            URIBuilder after = options.after(datas, URIBuilder.empty());

            if (datas.size() == 0) {
                hasNext = false;
            } else if (after != null) {
                options.setAfter(after.getParametersByName("after").get(0).getValue());
            }
        } while (hasNext);

        return all;
    }

    private int consumeAll(RecordRepository.Options options) throws ExecutionException, InterruptedException {
        return this.consumeAllRecord(options).size();
    }

    // Same page-by-page loop as consumeAllRecord, but bounds the number of pages so a broken 'after'
    // cursor (stuck cursor, infinite loop) fails fast instead of hanging the test, and returns just
    // the ordered keys for easy gap/duplicate assertions.
    private List<String> paginateConsume(RecordRepository.Options options) throws ExecutionException, InterruptedException {
        List<String> keys = new ArrayList<>();
        boolean hasNext = true;
        int pages = 0;

        do {
            assertTrue(pages++ <= 30, "Pagination did not terminate within a reasonable number of pages");

            List<Record> page = repository.consume(KafkaTestCluster.CLUSTER_ID, options);
            page.forEach(record -> keys.add(record.getKey()));

            URIBuilder after = options.after(page, URIBuilder.empty());
            if (page.isEmpty()) {
                hasNext = false;
            } else if (after != null) {
                options.setAfter(after.getParametersByName("after").get(0).getValue());
            }
        } while (hasNext);

        return keys;
    }

    // Pages through search() the way the SSE client does: follow the "after" cursor until a call
    // ends with no cursor at all.
    private List<String> paginateSearch(RecordRepository.Options options) throws ExecutionException, InterruptedException {
        Topic topic = topicRepository.findByName(options.getClusterId(), options.getTopic());
        List<String> keys = new ArrayList<>();
        AtomicBoolean hasNext = new AtomicBoolean(true);
        int calls = 0;

        while (hasNext.get()) {
            assertTrue(calls++ <= 40, "Search pagination did not terminate within a reasonable number of calls");

            repository.search(topic, options)
                .doOnNext(event -> {
                    event.getData().getRecords().forEach(record -> keys.add(record.getKey()));

                    if ("searchEnd".equals(event.getName())) {
                        if (event.getData().getAfter() == null) {
                            hasNext.set(false);
                        } else {
                            options.setAfter(event.getData().getAfter());
                        }
                    }
                })
                .blockLast();
        }

        return keys;
    }

    private RecordRepository.Options interleavedOptions(RecordRepository.Options.Sort sort, int size) {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_INTERLEAVED);
        options.setSort(sort);
        options.setSize(size);
        return options;
    }

    // Epoch millis of "key_<index>" in TOPIC_INTERLEAVED (timestamps are relative to injection time).
    private long interleavedTimestamp(int index) throws ExecutionException, InterruptedException {
        RecordRepository.Options options = interleavedOptions(RecordRepository.Options.Sort.OLDEST, KafkaTestCluster.TOPIC_INTERLEAVED_PER_PARTITION * 3);
        return repository.consume(KafkaTestCluster.CLUSTER_ID, options).stream()
            .filter(record -> record.getKey().equals("key_" + index))
            .findFirst()
            .orElseThrow()
            .getTimestamp().toInstant().toEpochMilli();
    }

    private List<String> interleavedKeys(boolean newestFirst, java.util.function.IntPredicate indexFilter) {
        int total = KafkaTestCluster.TOPIC_INTERLEAVED_PER_PARTITION * 3;
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            int index = newestFirst ? total - 1 - i : i;
            if (indexFilter.test(index)) {
                keys.add("key_" + index);
            }
        }
        return keys;
    }

    // Keys matched by searchByKey "key_1_C": key_1, key_10..key_19 and key_100..key_179. They are
    // unevenly spread across partitions, so pages mix partitions with and without matches.
    private static boolean containsKey1(int index) {
        return ("key_" + index).contains("key_1");
    }

    @Test
    void consumeOldestMultiPageWithFilterHasNoGapsOrDuplicates() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = interleavedOptions(RecordRepository.Options.Sort.OLDEST, 10);
        options.setSearchByKey("key_1_C");

        assertEquals(interleavedKeys(false, RecordRepositoryTest::containsKey1), paginateConsume(options));
    }

    @Test
    void consumeNewestMultiPageWithFilterHasNoGapsOrDuplicates() throws ExecutionException, InterruptedException {
        // Also checks ordering across pages: every partition must hand over its own newest matches
        // before the global sort, otherwise a later page could hold a record newer than an earlier one.
        RecordRepository.Options options = interleavedOptions(RecordRepository.Options.Sort.NEWEST, 10);
        options.setSearchByKey("key_1_C");

        assertEquals(interleavedKeys(true, RecordRepositoryTest::containsKey1), paginateConsume(options));
    }

    @Test
    void consumeOldestMultiPageWithTimestampRange() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = interleavedOptions(RecordRepository.Options.Sort.OLDEST, 10);
        options.setTimestamp(interleavedTimestamp(30));
        options.setEndTimestamp(interleavedTimestamp(149));

        assertEquals(interleavedKeys(false, i -> i >= 30 && i <= 149), paginateConsume(options));
    }

    @Test
    void consumeNewestMultiPageWithTimestampRange() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = interleavedOptions(RecordRepository.Options.Sort.NEWEST, 10);
        options.setTimestamp(interleavedTimestamp(30));
        options.setEndTimestamp(interleavedTimestamp(149));

        assertEquals(interleavedKeys(true, i -> i >= 30 && i <= 149), paginateConsume(options));
    }

    @Test
    void searchMultiPageWithFilterHasNoGapsOrDuplicates() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = interleavedOptions(RecordRepository.Options.Sort.OLDEST, 10);
        options.setSearchByKey("key_1_C");

        assertEquals(interleavedKeys(false, RecordRepositoryTest::containsKey1), paginateSearch(options));
    }

    @Test
    void searchMultiPageWithTimestampRange() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = interleavedOptions(RecordRepository.Options.Sort.OLDEST, 10);
        options.setTimestamp(interleavedTimestamp(30));
        options.setEndTimestamp(interleavedTimestamp(149));

        assertEquals(interleavedKeys(false, i -> i >= 30 && i <= 149), paginateSearch(options));
    }

    @Test
    void searchMultiPageWithPartitionFilter() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = interleavedOptions(RecordRepository.Options.Sort.OLDEST, 10);
        options.setPartition(1);

        assertEquals(interleavedKeys(false, i -> i % 3 == 1), paginateSearch(options));
    }

    // TOPIC_INTERLEAVED round-robins strictly increasing timestamps ("key_0".."key_(N-1)") across its
    // 3 partitions, so the oldest/newest order is simply ascending/descending key index.
    private List<String> expectedInterleavedKeys(boolean newestFirst) {
        int total = KafkaTestCluster.TOPIC_INTERLEAVED_PER_PARTITION * 3;
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            keys.add("key_" + (newestFirst ? total - 1 - i : i));
        }
        return keys;
    }

    @Test
    @Disabled("is flakky on github")
    void searchAll() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_HUGE);
        options.setSearchByKey("key_C");

        assertEquals(3000, searchAll(options));
    }

    @Test
    void searchKey() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_HUGE);
        options.setSearchByKey("key_100_C");

        assertEquals(3, searchAll(options));
    }

    @Test
    void searchValue() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_HUGE);
        options.setSearchByValue("value_100_C");

        assertEquals(3, searchAll(options));
    }

    @Test
    void searchOldestFirstPageIsGloballyOrderedAcrossPartitions() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(
            environment,
            KafkaTestCluster.CLUSTER_ID,
            KafkaTestCluster.TOPIC_INTERLEAVED
        );
        options.setSize(10);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        Topic topic = topicRepository.findByName(options.getClusterId(), options.getTopic());
        List<Record> firstPage = new ArrayList<>();
        repository.search(topic, options)
            .doOnNext(event -> {
                if ("searchBody".equals(event.getName())) {
                    firstPage.addAll(event.getData().getRecords());
                }
            })
            .blockLast();

        assertEquals(10, firstPage.size());
        assertEquals(
            List.of("key_0", "key_1", "key_2", "key_3", "key_4", "key_5", "key_6", "key_7", "key_8", "key_9"),
            firstPage.stream().map(Record::getKey).collect(Collectors.toList())
        );
        assertEquals(3, firstPage.stream().map(Record::getPartition).collect(Collectors.toSet()).size());
    }

    @Test
    void searchForcesOldestOrdering() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(
            environment,
            KafkaTestCluster.CLUSTER_ID,
            KafkaTestCluster.TOPIC_INTERLEAVED
        );
        options.setSize(10);
        options.setSort(RecordRepository.Options.Sort.NEWEST);

        Topic topic = topicRepository.findByName(options.getClusterId(), options.getTopic());
        List<Record> firstPage = new ArrayList<>();
        repository.search(topic, options)
            .doOnNext(event -> {
                if ("searchBody".equals(event.getName())) {
                    firstPage.addAll(event.getData().getRecords());
                }
            })
            .blockLast();

        assertEquals(10, firstPage.size());
        assertEquals(
            List.of("key_0", "key_1", "key_2", "key_3", "key_4", "key_5", "key_6", "key_7", "key_8", "key_9"),
            firstPage.stream().map(Record::getKey).collect(Collectors.toList())
        );
    }

    @Test
    void searchIncludesRecordAtInclusiveEndTimestamp() throws ExecutionException, InterruptedException {
        Record firstRecord = repository.consume(
            KafkaTestCluster.CLUSTER_ID,
            new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_INTERLEAVED)
        ).get(0);
        RecordRepository.Options options = new RecordRepository.Options(
            environment,
            KafkaTestCluster.CLUSTER_ID,
            KafkaTestCluster.TOPIC_INTERLEAVED
        );
        options.setTimestamp(firstRecord.getTimestamp().toInstant().toEpochMilli());
        options.setEndTimestamp(firstRecord.getTimestamp().toInstant().toEpochMilli());
        options.setSearchByKey(firstRecord.getKey() + "_E");

        assertEquals(1, searchAll(options));
    }

    @Test
    @org.junit.jupiter.api.Timeout(value = 30, unit = java.util.concurrent.TimeUnit.SECONDS)
    void searchMultiCallPaginationHasNoGapsOrDuplicates() throws ExecutionException, InterruptedException {
        // Unlike searchOldestFirstPageIsGloballyOrderedAcrossPartitions (single page), this forces many
        // separate search() calls (size=10 over 180 records) chained via the 'after' cursor, the same
        // way the SSE endpoint and the download loop page through a search. A cursor that is off by one
        // per-partition would show up here as a missing or repeated key.
        RecordRepository.Options options = new RecordRepository.Options(
            environment,
            KafkaTestCluster.CLUSTER_ID,
            KafkaTestCluster.TOPIC_INTERLEAVED
        );
        options.setSize(10);

        Topic topic = topicRepository.findByName(options.getClusterId(), options.getTopic());
        List<String> keys = new ArrayList<>();
        AtomicBoolean continueSearch = new AtomicBoolean(true);
        AtomicInteger calls = new AtomicInteger();

        while (continueSearch.get()) {
            assertTrue(calls.incrementAndGet() <= 30, "Search pagination did not terminate within a reasonable number of calls");

            repository.search(topic, options)
                .doOnNext(event -> {
                    event.getData().getRecords().forEach(record -> keys.add(record.getKey()));

                    if ("searchEnd".equals(event.getName()) && event.getData().getAfter() == null) {
                        continueSearch.set(false);
                    } else if (event.getData().getAfter() != null) {
                        options.setAfter(event.getData().getAfter());
                    }
                })
                .blockLast();
        }

        assertEquals(expectedInterleavedKeys(false), keys, "Search pagination must return every record exactly once, in ascending order");
    }

    @Test
    @org.junit.jupiter.api.Timeout(value = 15, unit = java.util.concurrent.TimeUnit.SECONDS)
    void searchNoMatchesTerminatesWithoutLooping() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(
            environment,
            KafkaTestCluster.CLUSTER_ID,
            KafkaTestCluster.TOPIC_INTERLEAVED
        );
        options.setSearchByKey("no_such_key_E");

        Topic topic = topicRepository.findByName(options.getClusterId(), options.getTopic());
        AtomicInteger totalRecords = new AtomicInteger();
        AtomicBoolean continueSearch = new AtomicBoolean(true);
        AtomicInteger calls = new AtomicInteger();

        while (continueSearch.get()) {
            // A full-topic scan with zero matches still needs one extra call to see an empty range
            // plan (see writeDownloadBatch's stop condition), so allow a couple of calls, not just one.
            assertTrue(calls.incrementAndGet() <= 3, "A non-matching search should terminate within a couple of calls");

            repository.search(topic, options)
                .doOnNext(event -> {
                    totalRecords.addAndGet(event.getData().getRecords().size());

                    if ("searchEnd".equals(event.getName()) && event.getData().getAfter() == null) {
                        continueSearch.set(false);
                    } else if (event.getData().getAfter() != null) {
                        options.setAfter(event.getData().getAfter());
                    }
                })
                .blockLast();
        }

        assertEquals(0, totalRecords.get());
    }

    @Test
    @org.junit.jupiter.api.Timeout(value = 30, unit = java.util.concurrent.TimeUnit.SECONDS)
    void searchDownloadLoopHandlesMultipleBatchesWithoutGapsOrDuplicates() throws ExecutionException, InterruptedException {
        // Same production loop shape as searchDownloadLoopTerminatesWhenResultFitsInOneCall, but with
        // more matches than one page (size=10 over 180 records), so the download endpoint's "else if"
        // continuation branch (options.setAfter(...) to fetch the next batch) is actually exercised,
        // not just its termination branch.
        RecordRepository.Options options = new RecordRepository.Options(
            environment,
            KafkaTestCluster.CLUSTER_ID,
            KafkaTestCluster.TOPIC_INTERLEAVED
        );
        options.setSize(10);

        Topic topic = topicRepository.findByName(options.getClusterId(), options.getTopic());

        AtomicBoolean continueSearch = new AtomicBoolean(true);
        List<String> keys = new ArrayList<>();
        AtomicInteger iterations = new AtomicInteger();

        while (continueSearch.get()) {
            assertTrue(iterations.incrementAndGet() <= 30, "download loop did not terminate");

            repository.search(topic, options)
                .doOnNext(event -> {
                    if (!event.getData().getRecords().isEmpty()) {
                        event.getData().getRecords().forEach(record -> keys.add(record.getKey()));
                        return;
                    }
                    // mirrors TopicController#writeDownloadBatch's production stop condition
                    if ("searchEnd".equals(event.getName()) && event.getData().getAfter() == null) {
                        continueSearch.set(false);
                    } else if (event.getData().getAfter() != null) {
                        options.setAfter(event.getData().getAfter());
                    }
                })
                .blockLast();
        }

        assertEquals(expectedInterleavedKeys(false), keys, "Download loop must return every record exactly once, in ascending order, across multiple batches");
    }

    @Test
    @org.junit.jupiter.api.Timeout(value = 30, unit = java.util.concurrent.TimeUnit.SECONDS)
    void searchDownloadLoopTerminatesWhenResultFitsInOneCall() throws ExecutionException, InterruptedException {
        // Regression test for the /data/download endpoint: it repeatedly calls search() and relies on
        // a "searchEnd" event with no "after" cursor to know the topic is fully drained (see
        // TopicController#writeDownloadBatch). A search whose only match is returned within a single
        // search() call (i.e. it never needs to hit its size quota) must still let a follow-up call
        // detect there is nothing left, instead of looping forever re-issuing empty searches.
        RecordRepository.Options options = new RecordRepository.Options(
            environment,
            KafkaTestCluster.CLUSTER_ID,
            KafkaTestCluster.TOPIC_INTERLEAVED
        );
        options.setSearchByKey("key_5_E");

        Topic topic = topicRepository.findByName(options.getClusterId(), options.getTopic());

        AtomicBoolean continueSearch = new AtomicBoolean(true);
        AtomicInteger totalRecords = new AtomicInteger();
        AtomicInteger iterations = new AtomicInteger();

        while (continueSearch.get()) {
            assertTrue(iterations.incrementAndGet() <= 5, "download-style loop did not terminate");

            repository.search(topic, options)
                .doOnNext(event -> {
                    if (!event.getData().getRecords().isEmpty()) {
                        totalRecords.addAndGet(event.getData().getRecords().size());
                        return;
                    }
                    if ("searchEnd".equals(event.getName()) && event.getData().getAfter() == null) {
                        continueSearch.set(false);
                    } else if (event.getData().getAfter() != null) {
                        options.setAfter(event.getData().getAfter());
                    }
                })
                .blockLast();
        }

        assertEquals(1, totalRecords.get());
    }

    @Test
    void searchValueSubject() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_STREAM_COUNT);
        options.setSearchByValueSubject("Count");

        assertEquals(12, searchAll(options));
    }

    @Test
    void searchAvro() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_STREAM_COUNT);
        options.setSearchByValue("count_C");

        assertEquals(12, searchAll(options));
    }

    @Test
    void lastRecordTest() throws ExecutionException, InterruptedException {
        Map<String, Record> record = repository.getLastRecord(KafkaTestCluster.CLUSTER_ID, Collections.singletonList(KafkaTestCluster.TOPIC_RANDOM));
        assertTrue(record.containsKey(KafkaTestCluster.TOPIC_RANDOM));
    }

    @Test
    void produceAndConsumeRecordUsingJsonSchema() throws ExecutionException, InterruptedException, IOException, RestClientException {
        Schema keyJsonSchema = registerSchema("json_schema/key.json", KafkaTestCluster.TOPIC_JSON_SCHEMA + "-key");
        Schema valueJsonSchema = registerSchema("json_schema/album.json", KafkaTestCluster.TOPIC_JSON_SCHEMA + "-value");
        Album objectSatisfyingJsonSchema = new Album("title", List.of("artist_1", "artist_2"), 1989, List.of("song_1", "song_2"));
        String recordAsJsonString = jsonMapper.writeValueAsString(objectSatisfyingJsonSchema);
        String keyJsonString = new JSONObject(Collections.singletonMap("id", "83fff9f8-b47a-4bf7-863b-9942c4369f06")).toString();

        RecordMetadata producedRecordMetadata = repository.produce(
            KafkaTestCluster.CLUSTER_ID,
            KafkaTestCluster.TOPIC_JSON_SCHEMA,
            Optional.of(recordAsJsonString),
            Collections.emptyList(),
            Optional.of(keyJsonString),
            Optional.empty(),
            Optional.empty(),
            Optional.of(KafkaTestCluster.TOPIC_JSON_SCHEMA + "-key"),
            Optional.of(KafkaTestCluster.TOPIC_JSON_SCHEMA + "-value")
        );

        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_JSON_SCHEMA);
        List<Record> records = consumeAllRecord(options);
        Optional<Record> consumedRecord = records.stream()
            .filter(record -> Objects.equals(record.getKey(), keyJsonString))
            .findFirst();
        assertTrue(consumedRecord.isPresent());
        Record recordToAssert = consumedRecord.get();
        assertEquals(recordToAssert.getKey(), keyJsonString);
        assertEquals(recordToAssert.getValue(), recordAsJsonString);
        assertEquals(recordToAssert.getValueSchemaId(), String.valueOf(valueJsonSchema.getId()));

        // clear schema registry as it is shared between tests
        schemaRegistryRepository.delete(KafkaTestCluster.CLUSTER_ID, keyJsonSchema.getSubject());
        schemaRegistryRepository.delete(KafkaTestCluster.CLUSTER_ID, valueJsonSchema.getSubject());
    }

    private Schema registerSchema(String resourcePath, String subject) throws IOException, RestClientException {
        String jsonSchemaRequest = ResourceTestUtil.resourceAsString(resourcePath);
        return schemaRegistryRepository.register(
            KafkaTestCluster.CLUSTER_ID,
            subject,
            "JSON",
            jsonSchemaRequest,
            Collections.emptyList()
        );
    }

    private int searchAll(RecordRepository.Options options) throws ExecutionException, InterruptedException {
        AtomicInteger size = new AtomicInteger();
        AtomicBoolean hasNext = new AtomicBoolean(true);

        Topic topic = topicRepository.findByName(options.getClusterId(), options.getTopic());

        do {
            repository.search(topic, options)
                .doOnNext(event -> {
                    size.addAndGet(event.getData().getRecords().size());

                    assertTrue(event.getData().getPercent() >= 0);
                    assertTrue(event.getData().getPercent() <= 100);

                    if (event.getName().equals("searchEnd")) {
                        if (event.getData().getAfter() == null) {
                            hasNext.set(false);
                        }
                    }

                    if (event.getData().getAfter() != null) {
                        options.setAfter(event.getData().getAfter());
                    }
                })
                .blockLast();

        } while (hasNext.get());

        return size.get();
    }

    @Test
    void copy() throws ExecutionException, InterruptedException, RestClientException, IOException {

        RecordRepository.Options optionsFromAndTo = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);

        Topic topicFromAndTo = topicRepository.findByName(optionsFromAndTo.getClusterId(), optionsFromAndTo.getTopic());

        List<TopicController.OffsetCopy> offsets = topicFromAndTo.getPartitions()
            .stream()
            .map(partition -> new TopicController.OffsetCopy(partition.getId(), partition.getLastOffset()))
            .collect(Collectors.toList());

        // We simulate the case a record has been added after the method copy has been used
        this.repository.produce(
            KafkaTestCluster.CLUSTER_ID,
            KafkaTestCluster.TOPIC_RANDOM,
            Optional.of("value"),
            Collections.emptyList(),
            Optional.of("key"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );

        RecordRepository.CopyResult copyResult = this.repository.copy(topicFromAndTo, KafkaTestCluster.CLUSTER_ID, topicFromAndTo, offsets, optionsFromAndTo);

        log.info("Copied " + copyResult.records + " records");

        assertEquals(300, copyResult.records);
    }
}
