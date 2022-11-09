package org.akhq.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.env.Environment;
import lombok.extern.slf4j.Slf4j;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.Record;
import org.akhq.models.Schema;
import org.akhq.models.Topic;
import org.akhq.utils.Album;
import org.akhq.utils.ResourceTestUtil;
import org.apache.kafka.clients.producer.RecordMetadata;
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
    private ObjectMapper objectMapper;

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
    void consumeNewest() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        options.setSort(RecordRepository.Options.Sort.NEWEST);

        assertEquals(300, consumeAll(options));
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


    @Test
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
        String recordAsJsonString = objectMapper.writeValueAsString(objectSatisfyingJsonSchema);
        String keyJsonString = new JSONObject(Collections.singletonMap("id", "83fff9f8-b47a-4bf7-863b-9942c4369f06")).toString();

        RecordMetadata producedRecordMetadata = repository.produce(
            KafkaTestCluster.CLUSTER_ID,
            KafkaTestCluster.TOPIC_JSON_SCHEMA,
            Optional.of(recordAsJsonString),
            Collections.emptyList(),
            Optional.of(keyJsonString),
            Optional.empty(),
            Optional.empty(),
            Optional.of(keyJsonSchema.getId()),
            Optional.of(valueJsonSchema.getId())
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
        assertEquals(recordToAssert.getValueSchemaId(), valueJsonSchema.getId());

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
            repository.search(topic, options).blockingSubscribe(event -> {
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
            });

        } while (hasNext.get());

        return size.get();
    }
}
