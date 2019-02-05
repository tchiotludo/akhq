package org.kafkahq.repositories;

import org.apache.kafka.common.config.TopicConfig;
import org.junit.Test;
import org.kafkahq.BaseTest;
import org.kafkahq.KafkaTestCluster;
import org.kafkahq.models.Config;
import org.kafkahq.models.Partition;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TopicRepositoryTest extends BaseTest {
    private final TopicRepository topicRepository = app.require(TopicRepository.class);
    private final ConfigRepository configRepository = app.require(ConfigRepository.class);

    @Test
    public void list() throws ExecutionException, InterruptedException {
        assertEquals(5, topicRepository.list(Optional.empty()).size());
    }

    @Test
    public void search() throws ExecutionException, InterruptedException {
        assertEquals(1, topicRepository.list(Optional.of("ra do")).size());
    }

    @Test
    public void create() throws ExecutionException, InterruptedException {
        topicRepository.create(KafkaTestCluster.CLUSTER_ID, "create", 8, (short) 1, Collections.singletonList(
            new Config(TopicConfig.SEGMENT_MS_CONFIG, "1000")
        ));

        Optional<String> option = configRepository.findByTopic("create")
            .stream()
            .filter(r -> r.getName().equals(TopicConfig.SEGMENT_MS_CONFIG))
            .findFirst()
            .map(Config::getValue);

        assertEquals(8, topicRepository.findByName("create").getPartitions().size());
        assertEquals("1000", option.get());

        topicRepository.delete(KafkaTestCluster.CLUSTER_ID, "create");
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

    @Test
    public void partition() throws ExecutionException, InterruptedException {
        assertEquals(3, topicRepository.findByName(KafkaTestCluster.TOPIC_COMPACTED).getPartitions().size());
    }
}