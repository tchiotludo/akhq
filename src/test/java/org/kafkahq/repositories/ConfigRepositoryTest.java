package org.kafkahq.repositories;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.kafkahq.AbstractTest;
import org.kafkahq.KafkaTestCluster;
import org.kafkahq.models.Config;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class ConfigRepositoryTest extends AbstractTest {
    @Inject
    private ConfigRepository repository;

    @Test
    public void updateTopic() throws ExecutionException, InterruptedException {
        repository.updateTopic(
            KafkaTestCluster.CLUSTER_ID,
            KafkaTestCluster.TOPIC_HUGE,
            Arrays.asList(
                new Config("file.delete.delay.ms", "1"),
                new Config("index.interval.bytes", "2")
            )
        );

        assertEquals("1", getConfig("file.delete.delay.ms").getValue());
        assertEquals("2", getConfig("index.interval.bytes").getValue());

        repository.updateTopic(
            KafkaTestCluster.CLUSTER_ID,
            KafkaTestCluster.TOPIC_HUGE,
            Collections.singletonList(
                new Config("file.delete.delay.ms", "3")
            )
        );

        assertEquals("3", getConfig("file.delete.delay.ms").getValue());
        assertEquals("2", getConfig("index.interval.bytes").getValue());


        repository.updateTopic(
            KafkaTestCluster.CLUSTER_ID,
            KafkaTestCluster.TOPIC_HUGE,
            Collections.singletonList(
                new Config("index.interval.bytes", "4")
            )
        );

        assertEquals("3", getConfig("file.delete.delay.ms").getValue());
        assertEquals("4", getConfig("index.interval.bytes").getValue());
    }

    private Config getConfig(String name) throws ExecutionException, InterruptedException {
        return repository
                .findByTopic(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_HUGE)
                .stream()
                .filter(config -> config.getName().equals(name))
                .findAny()
                .get();
    }
}
