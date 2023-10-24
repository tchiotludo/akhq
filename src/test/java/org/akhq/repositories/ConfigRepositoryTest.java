package org.akhq.repositories;

import org.junit.jupiter.api.Test;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.Config;

import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigRepositoryTest extends AbstractTest {
    @Inject
    private ConfigRepository repository;

    @Test
    void updateTopic() throws ExecutionException, InterruptedException {
        // write config the first time
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

        // update config 1
        repository.updateTopic(
            KafkaTestCluster.CLUSTER_ID,
            KafkaTestCluster.TOPIC_HUGE,
            Collections.singletonList(
                new Config("file.delete.delay.ms", "3")
            )
        );

        assertEquals("3", getConfig("file.delete.delay.ms").getValue());
        assertEquals("2", getConfig("index.interval.bytes").getValue());

        // update config 2
        repository.updateTopic(
            KafkaTestCluster.CLUSTER_ID,
            KafkaTestCluster.TOPIC_HUGE,
            Collections.singletonList(
                new Config("index.interval.bytes", "4")
            )
        );

        assertEquals("3", getConfig("file.delete.delay.ms").getValue());
        assertEquals("4", getConfig("index.interval.bytes").getValue());

        // reset config index.interval.bytes (leave config file.delete.delay.ms unchanged)
        repository.updateTopic(
            KafkaTestCluster.CLUSTER_ID,
            KafkaTestCluster.TOPIC_HUGE,
            Collections.singletonList(
                new Config("index.interval.bytes", "")
            )
        );

        Config unchangedConfig = getConfig("file.delete.delay.ms");
        assertTrue(unchangedConfig.getSource().name().startsWith("DYNAMIC_"));

        Config resettedConfig = getConfig("index.interval.bytes");
        assertTrue(resettedConfig.getSource().name().startsWith("DEFAULT_"));
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