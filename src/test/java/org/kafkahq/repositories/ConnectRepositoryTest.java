package org.kafkahq.repositories;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kafkahq.KafkaClusterExtension;
import org.kafkahq.KafkaTestCluster;
import org.kafkahq.models.ConnectDefinition;
import org.kafkahq.models.ConnectPlugin;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@ExtendWith(KafkaClusterExtension.class)
public class ConnectRepositoryTest {
    @Inject
    private ConnectRepository repository;

    @Test
    public void getPlugins() {
        List<ConnectPlugin> all = repository.getPlugins(KafkaTestCluster.CLUSTER_ID);
        assertEquals(2, all.size());
    }

    @Test
    public void getPlugin() {
        Optional<ConnectPlugin> plugin = repository.getPlugin(
            KafkaTestCluster.CLUSTER_ID,
            "FileStreamSinkConnector"
        );

        assertTrue(plugin.isPresent());
        assertEquals("FileStreamSinkConnector", plugin.get().getShortClassName());
        assertEquals("sink", plugin.get().getType());
        assertTrue(plugin.get().getDefinitions().stream().anyMatch(definition -> definition.getName().equals("file")));
    }

    @BeforeEach
    public void cleanup() {
        try {
            repository.delete(KafkaTestCluster.CLUSTER_ID, "ConnectRepositoryTest");
        } catch (Exception ignored) {
        }
    }

    @Test
    public void create() {
        repository.create(
            KafkaTestCluster.CLUSTER_ID,
            "ConnectRepositoryTest",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", "/etc/hosts",
                "topics", "test-topics1"
            )
        );

        List<ConnectDefinition> all = repository.getDefinitions(KafkaTestCluster.CLUSTER_ID);
        assertEquals(1, all.size());

        assertEquals("/etc/hosts", repository.getDefinition(
            KafkaTestCluster.CLUSTER_ID,
            "ConnectRepositoryTest"
        ).getConfigs().get("file"));

        repository.update(
            KafkaTestCluster.CLUSTER_ID,
            "ConnectRepositoryTest",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", "/etc/resolv.conf ",
                "topics", "test-topics1"
            )
        );

        assertEquals("/etc/resolv.conf ", repository.getDefinition(
            KafkaTestCluster.CLUSTER_ID,
            "ConnectRepositoryTest"
        ).getConfigs().get("file"));

        repository.delete(KafkaTestCluster.CLUSTER_ID, "ConnectRepositoryTest");
        assertEquals(0, repository.getDefinitions(KafkaTestCluster.CLUSTER_ID).size());
    }
}
