package org.akhq.repositories;

import io.micronaut.context.env.Environment;
import lombok.extern.slf4j.Slf4j;
import org.akhq.models.Topic;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.Record;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class RecordRepositoryTest extends AbstractTest {
    @Inject
    private RecordRepository repository;

    @Inject
    private TopicRepository topicRepository;

    @Inject
    private Environment environment;
    
    @Test
    public void consumeEmpty() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_EMPTY);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        assertEquals(0, consumeAll(options));
    }

    @Test
    public void consumeOldest() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        assertEquals(300, consumeAll(options));
    }

    @Test
    public void consumeNewest() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        options.setSort(RecordRepository.Options.Sort.NEWEST);

        assertEquals(300, consumeAll(options));
    }

    @Test
    public void consumeOldestPerPartition() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        options.setSort(RecordRepository.Options.Sort.OLDEST);
        options.setPartition(1);

        assertEquals(100, consumeAll(options));
    }

    @Test
    public void consumeNewestPerPartition() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        options.setSort(RecordRepository.Options.Sort.NEWEST);
        options.setPartition(1);

        assertEquals(100, consumeAll(options));
    }

    @Test
    public void consumeOldestCompacted() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        assertEquals(153, consumeAll(options));
    }

    @Test
    public void consumeNewestCompacted() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED);
        options.setSort(RecordRepository.Options.Sort.NEWEST);

        assertEquals(153, consumeAll(options));
    }

    @Test
    public void consumeOldestPerPartitionCompacted() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED);
        options.setSort(RecordRepository.Options.Sort.OLDEST);
        options.setPartition(0);

        assertEquals(51, consumeAll(options));
    }

    @Test
    public void consumeNewestPerPartitionCompacted() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED);
        options.setSort(RecordRepository.Options.Sort.NEWEST);
        options.setPartition(0);

        assertEquals(51, consumeAll(options));
    }


    @Test
    public void consumeAvro() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_STREAM_MAP);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        List<Record> records = consumeAllRecord(options);

        assertEquals(12, records.size());


        Optional<Record> avroRecord = records
            .stream()
            .filter(record -> record.getKey().equals("1"))
            .findFirst();

        avroRecord.orElseThrow(() -> new NoSuchElementException("Unable to find key 1"));
        avroRecord.ifPresent(record -> assertEquals("{\"id\":1,\"name\":\"WaWa\",\"breed\":\"ABYSSINIAN\"}", record.getValue()));
    }

    @Test
    public void emptyTopic() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_TOBE_EMPTIED);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        repository.emptyTopic(options.clusterId, options.getTopic());
        assertEquals(0, consumeAll(options) );
    }

    @Disabled("Method not ready yet")
    @Test
    public void emptyTopicByTimestamp() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_TOBE_EMPTIED);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        repository.emptyTopicByTimestamp(options.clusterId, options.getTopic(), System.currentTimeMillis());
        assertEquals(0, consumeAll(options) );
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
    public void searchAll() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_HUGE);
        options.setSearchByKey("key_C");

        assertEquals(3000, searchAll(options));
    }

    @Test
    public void searchKey() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_HUGE);
        options.setSearchByKey("key_100_C");

        assertEquals(3, searchAll(options));
    }

    @Test
    public void searchValue() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_HUGE);
        options.setSearchByValue("value_100_C");

        assertEquals(3, searchAll(options));
    }

    @Test
    public void searchAvro() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(environment, KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_STREAM_COUNT);
        options.setSearchByValue("count_C");

        assertEquals(12, searchAll(options));
    }

    @Test
    void lastRecordTest() throws ExecutionException, InterruptedException {
        Map<String, Record> record = repository.getLastRecord(KafkaTestCluster.CLUSTER_ID, Collections.singletonList(KafkaTestCluster.TOPIC_RANDOM));
        assertTrue(record.containsKey(KafkaTestCluster.TOPIC_RANDOM));
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
