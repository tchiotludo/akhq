package org.kafkahq.repositories;

import org.codehaus.httpcache4j.uri.URIBuilder;
import org.junit.Test;
import org.kafkahq.BaseTest;
import org.kafkahq.KafkaTestCluster;
import org.kafkahq.models.Record;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class RecordRepositoryTest extends BaseTest {
    private final RecordRepository repository = app.require(RecordRepository.class);

    @Test
    public void consumeEmpty() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_EMPTY);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        assertEquals(0, consumeAll(repository, options));
    }

    @Test
    public void consumeOldest() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        assertEquals(300, consumeAll(repository, options));
    }

    @Test
    public void consumeNewest() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        options.setSort(RecordRepository.Options.Sort.NEWEST);

        assertEquals(300, consumeAll(repository, options));
    }

    @Test
    public void consumeOldestPerPartition() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        options.setSort(RecordRepository.Options.Sort.OLDEST);
        options.setPartition(1);

        assertEquals(100, consumeAll(repository, options));
    }

    @Test
    public void consumeNewestPerPartition() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_RANDOM);
        options.setSort(RecordRepository.Options.Sort.NEWEST);
        options.setPartition(1);

        assertEquals(100, consumeAll(repository, options));
    }

    @Test
    public void consumeOldestCompacted() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED);
        options.setSort(RecordRepository.Options.Sort.OLDEST);

        assertEquals(153, consumeAll(repository, options));
    }

    @Test
    public void consumeNewestCompacted() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED);
        options.setSort(RecordRepository.Options.Sort.NEWEST);

        assertEquals(153, consumeAll(repository, options));
    }

    @Test
    public void consumeOldestPerPartitionCompacted() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED);
        options.setSort(RecordRepository.Options.Sort.OLDEST);
        options.setPartition(0);

        assertEquals(51, consumeAll(repository, options));
    }

    @Test
    public void consumeNewestPerPartitionCompacted() throws ExecutionException, InterruptedException {
        RecordRepository.Options options = new RecordRepository.Options(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED);
        options.setSort(RecordRepository.Options.Sort.NEWEST);
        options.setPartition(0);

        assertEquals(51, consumeAll(repository, options));
    }

    private int consumeAll(RecordRepository repository, RecordRepository.Options options) throws ExecutionException, InterruptedException {
        int size = 0;
        boolean hasNext = true;

        do {
            List<Record<String, String>> datas = repository.consume(options);
            size += datas.size();

            datas.forEach(record -> logger.debug(
                "Records [Topic: {}] [Partition: {}] [Offset: {}] [Key: {}] [Value: {}]",
                record.getTopic(),
                record.getPartition(),
                record.getOffset(),
                record.getKey(),
                record.getValue()
            ));
            logger.info("Consume {} records", datas.size());

            URIBuilder after = options.after(datas, URIBuilder.empty());

            if (datas.size() == 0) {
                hasNext = false;
            } else {
                options.setAfter(after.getParametersByName("after").get(0).getValue());
            }
        } while (hasNext);

        return size;
    }

}