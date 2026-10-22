package org.akhq.repositories;

import io.micronaut.context.ApplicationContext;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.ServerAuthentication;
import io.micronaut.security.utils.DefaultSecurityService;
import io.micronaut.security.utils.SecurityService;
import com.google.common.collect.ImmutableMap;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.clients.connect.KafkaConnectApiClient;
import org.akhq.clients.connect.dto.ConnectorExpanded;
import org.akhq.clients.connect.dto.ConnectorInfo;
import org.akhq.clients.connect.dto.ConnectorStatus;
import org.akhq.models.ConnectDefinition;
import org.akhq.models.ConnectPlugin;
import org.akhq.modules.KafkaModule;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import jakarta.inject.Inject;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ConnectRepositoryTest extends AbstractTest {

    private static final Duration CONNECT_OPERATION_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration CONNECT_OPERATION_POLL_INTERVAL = Duration.ofMillis(1);

    @Inject
    @InjectMocks
    private ConnectRepository repository;

    @Mock
    ApplicationContext applicationContext;

    private ExecutorService pagedExecutor;

    @BeforeEach
    void before() {
        pagedExecutor = Executors.newFixedThreadPool(4);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void getPlugins() {
        List<ConnectPlugin> all = repository.getPlugins(KafkaTestCluster.CLUSTER_ID, "connect-1");
        assertEquals(5, all.size());
    }

    @AfterEach
    void cleanup() {
        pagedExecutor.shutdownNow();
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

    @Test
    void paginatedDefinitionsOnlyFetchRequestedPageAfterSearchAndAclFiltering() throws Exception {
        KafkaConnectApiClient client = mockClient();
        List<String> matching = IntStream.rangeClosed(1, 12)
            .mapToObj(i -> String.format("match-%02d", i))
            .collect(Collectors.toList());
        List<String> all = new ArrayList<>(matching);
        all.add("other-1");
        all.add("match-denied");
        Collections.shuffle(all, new Random(42));
        when(client.getConnectorNames()).thenReturn(all);

        PagedList<ConnectDefinition> page2 = pagedRepository(client).getPaginatedDefinitions(
            "cluster", "connect", new Pagination(5, URIBuilder.empty(), 2),
            Optional.of("match"), Optional.empty(), List.of("match-\\d+")
        );

        List<String> expectedPage = matching.subList(5, 10);
        assertEquals(expectedPage, page2.stream().map(ConnectDefinition::getName).collect(Collectors.toList()));
        assertEquals(matching.size(), page2.total());

        for (String name : all) {
            int expectedCalls = expectedPage.contains(name) ? 1 : 0;
            verify(client, times(expectedCalls)).getConnector(name);
            verify(client, times(expectedCalls)).getConnectorStatus(name);
        }
        verify(client, never()).getConnectorsStatus();
    }

    @Test
    void paginatedDefinitionsFetchOnlyLastPage() throws Exception {
        KafkaConnectApiClient client = mockClient();
        when(client.getConnectorNames()).thenReturn(names(12));

        PagedList<ConnectDefinition> page3 = pagedRepository(client).getPaginatedDefinitions(
            "cluster", "connect", new Pagination(5, URIBuilder.empty(), 3),
            Optional.empty(), Optional.empty(), List.of()
        );

        assertEquals(List.of("c-11", "c-12"), page3.stream().map(ConnectDefinition::getName).collect(Collectors.toList()));
        assertEquals(12, page3.total());
        verify(client, times(2)).getConnector(anyString());
        verify(client, never()).getConnectorsStatus();
    }

    @Test
    void paginatedDefinitionsWithStatusFilterKeepFullFilteredTotal() throws Exception {
        KafkaConnectApiClient client = mockClient();
        List<String> names = names(12);
        when(client.getConnectorNames()).thenReturn(names);

        Map<String, ConnectorExpanded> statuses = new HashMap<>();
        names.forEach(name -> {
            ConnectorExpanded expanded = new ConnectorExpanded();
            expanded.setStatus(connectorStatus(name, Integer.parseInt(name.substring(2)) % 2 == 0 ? "FAILED" : "RUNNING"));
            statuses.put(name, expanded);
        });
        when(client.getConnectorsStatus()).thenReturn(statuses);

        PagedList<ConnectDefinition> page1 = pagedRepository(client).getPaginatedDefinitions(
            "cluster", "connect", new Pagination(5, URIBuilder.empty(), 1),
            Optional.empty(), Optional.of("failed"), List.of()
        );

        assertEquals(List.of("c-02", "c-04", "c-06", "c-08", "c-10"),
            page1.stream().map(ConnectDefinition::getName).collect(Collectors.toList()));
        assertEquals(6, page1.total());
        verify(client, times(1)).getConnectorsStatus();
        verify(client, times(5)).getConnector(anyString());
        verify(client, never()).getConnector("c-12");
    }

    private KafkaConnectApiClient mockClient() {
        KafkaConnectApiClient client = Mockito.mock(KafkaConnectApiClient.class);
        when(client.getConnector(anyString())).thenAnswer(i -> {
            ConnectorInfo info = new ConnectorInfo();
            info.setName(i.getArgument(0));
            info.setType("sink");
            info.setConfig(Map.of());
            info.setTasks(List.of());
            return info;
        });
        when(client.getConnectorStatus(anyString())).thenAnswer(i -> connectorStatus(i.getArgument(0), "RUNNING"));
        return client;
    }

    private ConnectRepository pagedRepository(KafkaConnectApiClient client) throws Exception {
        KafkaModule kafkaModule = Mockito.mock(KafkaModule.class);
        when(kafkaModule.getConnectRestClient("cluster")).thenReturn(Map.of("connect", client));

        ConnectRepository mocked = new ConnectRepository();
        setField(mocked, "kafkaModule", kafkaModule);
        setField(mocked, "executorService", pagedExecutor);
        return mocked;
    }

    private static void setField(ConnectRepository target, String name, Object value) throws Exception {
        Field field = ConnectRepository.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static List<String> names(int count) {
        return IntStream.rangeClosed(1, count)
            .mapToObj(i -> String.format("c-%02d", i))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private static ConnectorStatus connectorStatus(String name, String taskState) {
        ConnectorStatus status = new ConnectorStatus();
        status.setName(name);
        ConnectorStatus.ConnectorState connector = new ConnectorStatus.ConnectorState();
        connector.setState("RUNNING");
        status.setConnector(connector);
        ConnectorStatus.TaskState task = new ConnectorStatus.TaskState();
        task.setId(0);
        task.setState(taskState);
        status.setTasks(List.of(task));
        return status;
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
