package org.akhq.repositories;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.ConnectDefinition;
import org.akhq.models.ConnectPlugin;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class ConnectRepositoryTest extends AbstractTest {
    @Inject
    private ConnectRepository repository;

    @Test
    public void getPlugins() {
        List<ConnectPlugin> all = repository.getPlugins(KafkaTestCluster.CLUSTER_ID, "connect-1");
        assertEquals(2, all.size());
    }

    @Test
    public void getPlugin() {
        Optional<ConnectPlugin> plugin = repository.getPlugin(
            KafkaTestCluster.CLUSTER_ID,
            "connect-1",
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
            repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-1", "ConnectRepositoryTest1");
            repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-2", "ConnectRepositoryTest2");
        } catch (Exception ignored) {
        }
    }

    @Test
    public void create() {
        String path1 = ConnectRepository.class.getClassLoader().getResource("application.yml").getPath();
        String path2 = ConnectRepository.class.getClassLoader().getResource("logback.xml").getPath();

        repository.create(
            KafkaTestCluster.CLUSTER_ID,
            "connect-1",
            "ConnectRepositoryTest1",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", path1,
                "topics", "test-topics1"
            )
        );

        repository.create(
            KafkaTestCluster.CLUSTER_ID,
            "connect-2",
            "ConnectRepositoryTest2",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", path1,
                "topics", "test-topics1"
            )
        );


        List<ConnectDefinition> all1 = repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-1");
        assertEquals(1, all1.size());

        List<ConnectDefinition> all2 = repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-2");
        assertEquals(1, all2.size());

        assertEquals(path1, repository.getDefinition(
            KafkaTestCluster.CLUSTER_ID,
            "connect-1",
            "ConnectRepositoryTest1"
        ).getConfigs().get("file"));

        assertEquals(path1, repository.getDefinition(
            KafkaTestCluster.CLUSTER_ID,
            "connect-2",
            "ConnectRepositoryTest2"
        ).getConfigs().get("file"));

        repository.update(
            KafkaTestCluster.CLUSTER_ID,
            "connect-1",
            "ConnectRepositoryTest1",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", path2,
                "topics", "test-topics1"
            )
        );

        repository.update(
            KafkaTestCluster.CLUSTER_ID,
            "connect-2",
            "ConnectRepositoryTest2",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", path2,
                "topics", "test-topics1"
            )
        );

        assertEquals(path2, repository.getDefinition(
            KafkaTestCluster.CLUSTER_ID,
            "connect-1",
            "ConnectRepositoryTest1"
        ).getConfigs().get("file"));

        assertEquals(path2, repository.getDefinition(
            KafkaTestCluster.CLUSTER_ID,
            "connect-2",
            "ConnectRepositoryTest2"
        ).getConfigs().get("file"));

        repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-1","ConnectRepositoryTest1");
        repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-2","ConnectRepositoryTest2");
        assertEquals(0, repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-1").size());
        assertEquals(0, repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-2").size());
    }
}
