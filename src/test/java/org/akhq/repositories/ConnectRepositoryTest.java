package org.akhq.repositories;

import io.micronaut.context.ApplicationContext;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.ServerAuthentication;
import io.micronaut.security.utils.DefaultSecurityService;
import io.micronaut.security.utils.SecurityService;
import com.google.common.collect.ImmutableMap;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.ConnectDefinition;
import org.akhq.models.ConnectPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import jakarta.inject.Inject;
import java.time.Duration;
import java.util.*;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class ConnectRepositoryTest extends AbstractTest {

    private static final Duration CONNECT_OPERATION_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration CONNECT_OPERATION_POLL_INTERVAL = Duration.ofMillis(1);

    @Inject
    @InjectMocks
    private ConnectRepository repository;

    @Mock
    ApplicationContext applicationContext;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void getPlugins() {
        List<ConnectPlugin> all = repository.getPlugins(KafkaTestCluster.CLUSTER_ID, "connect-1");
        assertEquals(5, all.size());
    }

    @AfterEach
    void cleanup() {
        try {
            repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-1", "ConnectRepositoryTest1");
            repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-2", "ConnectRepositoryTest2");
        } catch (Exception ignored) {
        }
    }

    @Test
    void create() {
        String path1 = ConnectRepository.class.getClassLoader().getResource("application.yml").getPath();
        String path2 = ConnectRepository.class.getClassLoader().getResource("logback.xml").getPath();

        createWithRetry(
            "connect-1",
            "ConnectRepositoryTest1",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", path1,
                "topics", KafkaTestCluster.TOPIC_CONNECT
            )
        );

        createWithRetry(
            "connect-2",
            "ConnectRepositoryTest2",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", path1,
                "topics", KafkaTestCluster.TOPIC_CONNECT
            )
        );

        await().atMost(CONNECT_OPERATION_TIMEOUT).pollInterval(CONNECT_OPERATION_POLL_INTERVAL).untilAsserted(() -> {
                List<ConnectDefinition> all1 =
                    repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-1", Optional.empty(), Optional.empty(),
                        List.of());
                assertEquals(1, all1.size());
            });

        List<ConnectDefinition> all2 = repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-2", Optional.empty(), Optional.empty(), List.of());
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

        updateWithRetry(
            "connect-1",
            "ConnectRepositoryTest1",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", path2,
                "topics", KafkaTestCluster.TOPIC_CONNECT
            )
        );

        updateWithRetry(
            "connect-2",
            "ConnectRepositoryTest2",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", path2,
                "topics", KafkaTestCluster.TOPIC_CONNECT
            )
        );

        await().atMost(CONNECT_OPERATION_TIMEOUT).pollInterval(CONNECT_OPERATION_POLL_INTERVAL).untilAsserted(() ->
            assertEquals(path2, repository.getDefinition(
                KafkaTestCluster.CLUSTER_ID,
                "connect-1",
                "ConnectRepositoryTest1"
        ).getConfigs().get("file")));

        assertEquals(path2, repository.getDefinition(
            KafkaTestCluster.CLUSTER_ID,
            "connect-2",
            "ConnectRepositoryTest2"
        ).getConfigs().get("file"));

        repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-1","ConnectRepositoryTest1");
        repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-2","ConnectRepositoryTest2");

        await().atMost(CONNECT_OPERATION_TIMEOUT).pollInterval(CONNECT_OPERATION_POLL_INTERVAL).untilAsserted(() -> {
            assertEquals(0,
                repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-1", Optional.empty(), Optional.empty(),
                    List.of()).size());
            assertEquals(0,
                repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-2", Optional.empty(), Optional.empty(),
                    List.of()).size());
        });
    }

    @Test
    void getFilteredList() {

        createWithRetry(
            "connect-1",
            "prefixed.Matching1",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", "/tmp/test.txt",
                "topics", KafkaTestCluster.TOPIC_CONNECT
            )
        );

        createWithRetry(
            "connect-1",
            "prefixed.Matching2",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", "/tmp/test.txt",
                "topics", KafkaTestCluster.TOPIC_CONNECT
            )
        );

        createWithRetry(
            "connect-1",
            "not.Matching3",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", "/tmp/test.txt",
                "topics", KafkaTestCluster.TOPIC_CONNECT
            )
        );

        mockApplicationContext();

        await().atMost(CONNECT_OPERATION_TIMEOUT).pollInterval(CONNECT_OPERATION_POLL_INTERVAL).untilAsserted(() -> {
                List<ConnectDefinition> filtered =
                    repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-1", Optional.empty(), Optional.empty(),
                        List.of("^prefixed.*$"));
                assertEquals(2, filtered.size());
            });

        repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-1", "prefixed.Matching1");
        repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-1", "prefixed.Matching2");
        repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-1", "not.Matching3");
    }


    @Test
    void getFilteredBySearchList() {

        createWithRetry(
            "connect-1",
                "prefixed.Matching1",
                ImmutableMap.of(
                        "connector.class", "FileStreamSinkConnector",
                        "file", "/tmp/test.txt",
                        "topics", KafkaTestCluster.TOPIC_CONNECT
                )
        );

        createWithRetry(
            "connect-1",
                "prefixed.Matching2",
                ImmutableMap.of(
                        "connector.class", "FileStreamSinkConnector",
                        "file", "/tmp/test.txt",
                        "topics", KafkaTestCluster.TOPIC_CONNECT
                )
        );

        mockApplicationContext();

        await().atMost(CONNECT_OPERATION_TIMEOUT).pollInterval(CONNECT_OPERATION_POLL_INTERVAL).untilAsserted(() -> {
                List<ConnectDefinition> notFiltered =
                    repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-1", Optional.empty(), Optional.empty(),
                        List.of());
                assertEquals(2, notFiltered.size());
            });
        List<ConnectDefinition> filtered = repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-1", Optional.of("prefixed.Matching1"), Optional.empty(), List.of());
        assertEquals(1, filtered.size());
        List<ConnectDefinition> filteredAll = repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-1", Optional.of("prefixed.Matching"), Optional.empty(), List.of());
        assertEquals(2, filteredAll.size());

        repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-1", "prefixed.Matching1");
        repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-1", "prefixed.Matching2");
    }

    @Test
    void getFilteredByStatusList() {
        createWithRetry(
            "connect-1",
            "statusTest1",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", "/tmp/test.txt",
                "topics", KafkaTestCluster.TOPIC_CONNECT
            )
        );

        mockApplicationContext();

        await().atMost(CONNECT_OPERATION_TIMEOUT).pollInterval(CONNECT_OPERATION_POLL_INTERVAL).untilAsserted(() -> {
            List<ConnectDefinition> all = repository.getDefinitions(
                KafkaTestCluster.CLUSTER_ID,
                "connect-1",
                Optional.empty(),
                Optional.empty(),
                List.of()
            );
            assertEquals(1, all.size());
            assertEquals(1, all.getFirst().getTasks().size());
            assertEquals("RUNNING", all.getFirst().getTasks().getFirst().getState());
        });

        // Filtering by the RUNNING state
        List<ConnectDefinition> matched = repository.getDefinitions(
            KafkaTestCluster.CLUSTER_ID, "connect-1", Optional.empty(), Optional.of("RUNNING"), List.of());
        assertEquals(1, matched.size());

        // Filtering by a different state should return nothing
        List<ConnectDefinition> notMatched = repository.getDefinitions(
            KafkaTestCluster.CLUSTER_ID, "connect-1", Optional.empty(), Optional.of("FAILED"), List.of());
        assertEquals(0, notMatched.size());

        repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-1", "statusTest1");
    }

    private void mockApplicationContext() {
        Authentication auth = new ServerAuthentication("test", List.of(), Map.of());
        DefaultSecurityService securityService = Mockito.mock(DefaultSecurityService.class);
        when(securityService.getAuthentication()).thenReturn(Optional.of(auth));
        when(applicationContext.containsBean(SecurityService.class)).thenReturn(true);
        when(applicationContext.getBean(SecurityService.class)).thenReturn(securityService);
    }

    private void createWithRetry(String connectId, String name, Map<String, String> configs) {
        await()
            .atMost(CONNECT_OPERATION_TIMEOUT)
            .pollInterval(CONNECT_OPERATION_POLL_INTERVAL)
            .untilAsserted(() -> repository.create(KafkaTestCluster.CLUSTER_ID, connectId, name, configs));
    }

    private void updateWithRetry(String connectId, String name, Map<String, String> configs) {
        await()
            .atMost(CONNECT_OPERATION_TIMEOUT)
            .pollInterval(CONNECT_OPERATION_POLL_INTERVAL)
            .untilAsserted(() -> repository.update(KafkaTestCluster.CLUSTER_ID, connectId, name, configs));
    }
}
