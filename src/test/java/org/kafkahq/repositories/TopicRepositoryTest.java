package org.kafkahq.repositories;

import org.junit.Test;
import org.kafkahq.BaseTest;
import org.kafkahq.KafkaTestCluster;
import org.kafkahq.models.Partition;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TopicRepositoryTest extends BaseTest {
    private final TopicRepository topicRepository = app.require(TopicRepository.class);

    @Test
    public void list() throws ExecutionException, InterruptedException {
        assertEquals(3, topicRepository.list().size());
    }

    @Test
    public void partition() throws ExecutionException, InterruptedException {
        assertEquals(3, topicRepository.findByName(KafkaTestCluster.TOPIC_COMPACTED).getPartitions().size());
    }

    @Test
    public void offset() throws ExecutionException, InterruptedException {
        Optional<Partition> compacted = topicRepository
            .findByName(KafkaTestCluster.TOPIC_COMPACTED)
            .getPartitions()
            .stream()
            .filter(partition -> partition.getId() == 0)
            .findFirst();

        assertTrue(compacted.isPresent());
        assertEquals(0, compacted.get().getFirstOffset());
        assertEquals(100, compacted.get().getLastOffset());
    }
}