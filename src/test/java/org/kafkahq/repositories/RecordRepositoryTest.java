package org.kafkahq.repositories;

import lombok.extern.slf4j.Slf4j;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.junit.Test;
import org.kafkahq.BaseTest;
import org.kafkahq.KafkaTestCluster;
import org.kafkahq.models.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

@Slf4j
public class RecordRepositoryTest extends BaseTest {
    private final RecordRepository repository = app.require(RecordRepository.class);

    @Test
    public void consumeEmpty() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_EMPTY);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        assertEquals(0, consumeAll(options));
    }

    @Test
    public void consumeOldest() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        assertEquals(300, consumeAll(options));
    }

    @Test
    public void consumeNewest() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        options.setSort(RecordRepository.Options.Sort.NEWEST);

        assertEquals(300, consumeAll(options));
    }

    @Test
    public void consumeOldestPerPartition() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        options.setSort(RecordRepository.Options.Sort.OLDEST);
        options.setPartition(1);

        assertEquals(100, consumeAll(options));
    }

    @Test
    public void consumeNewestPerPartition() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        options.setSort(RecordRepository.Options.Sort.NEWEST);
        options.setPartition(1);

        assertEquals(100, consumeAll(options));
    }

    @Test
    public void consumeOldestCompacted() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        assertEquals(153, consumeAll(options));
    }

    @Test
    public void consumeNewestCompacted() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED);
        options.setSort(RecordRepository.Options.Sort.NEWEST);

        assertEquals(153, consumeAll(options));
    }

    @Test
    public void consumeOldestPerPartitionCompacted() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED);
        options.setSort(RecordRepository.Options.Sort.OLDEST);
        options.setPartition(0);

        assertEquals(51, consumeAll(options));
    }

    @Test
    public void consumeNewestPerPartitionCompacted() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED);
        options.setSort(RecordRepository.Options.Sort.NEWEST);
        options.setPartition(0);

        assertEquals(51, consumeAll(options));
    }


    @Test
    public void consumeAvro() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_STREAM_MAP);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        List<Record> records = consumeAllRecord(options);

        assertEquals(12, records.size());


        Optional<Record> avroRecord = records
            .stream()
            .filter(record -> record.getKeyAsString().equals("1"))
            .findFirst();

        avroRecord.orElseThrow(() -> new NoSuchElementException("Unable to find key 1"));
        avroRecord.ifPresent(record -> assertEquals("{\"id\": 1, \"name\": \"WaWa\", \"breed\": \"ABYSSINIAN\"}", record.getValueAsString()));
    }

    private List<Record> consumeAllRecord(RecordRepository.Options options) throws ExecutionException, InterruptedException {
        boolean hasNext = true;

        List<Record> all = new ArrayList<>();

        do {
            List<Record> datas = repository.consume(options);
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
            } else {
                options.setAfter(after.getParametersByName("after").get(0).getValue());
            }
        } while (hasNext);

        return all;
    }

    private int consumeAll(RecordRepository.Options options) throws ExecutionException, InterruptedException {
        return this.consumeAllRecord(options).size();
    }

    @Test
    public void searchAll() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_HUGE);
        options.setSearch("key");

        assertEquals(3000, searchAll(options));
    }

    @Test
    public void searchKey() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_HUGE);
        options.setSearch("key_100");

        assertEquals(3, searchAll(options));
    }

    @Test
    public void searchValue() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_HUGE);
        options.setSearch("value_100");

        assertEquals(3, searchAll(options));
    }

    private int searchAll(RecordRepository.Options options) throws ExecutionException, InterruptedException {
        int size = 0;
        boolean hasNext = true;

        do {
            SearchConsumer searchConsumer = new SearchConsumer();
            RecordRepository.SearchEnd end = repository.search(options, searchConsumer);
            size += searchConsumer.size;

            if (end.getAfter() == null) {
                hasNext = false;
            } else {
                options.setAfter(end.getAfter());
            }
        } while (hasNext);

        return size;
    }

    private static class SearchConsumer extends RecordRepository.SearchConsumer {
        public int size = 0;

        @Override
        public void accept(RecordRepository.SearchEvent searchEvent) {
            size += searchEvent.getRecords().size();
        }
    }
}