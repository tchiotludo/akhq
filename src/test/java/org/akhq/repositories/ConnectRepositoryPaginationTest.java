package org.akhq.repositories;

import org.akhq.clients.connect.KafkaConnectApiClient;
import org.akhq.clients.connect.dto.ConnectorExpanded;
import org.akhq.clients.connect.dto.ConnectorInfo;
import org.akhq.clients.connect.dto.ConnectorStatus;
import org.akhq.models.ConnectDefinition;
import org.akhq.modules.KafkaModule;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ConnectRepositoryPaginationTest {
    private static final String CLUSTER = "cluster";
    private static final String CONNECT = "connect";
    private static final int PAGE_SIZE = 5;

    private final KafkaConnectApiClient client = mock(KafkaConnectApiClient.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private ConnectRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        KafkaModule kafkaModule = mock(KafkaModule.class);
        when(kafkaModule.getConnectRestClient(CLUSTER)).thenReturn(Map.of(CONNECT, client));

        repository = new ConnectRepository();
        inject("kafkaModule", kafkaModule);
        inject("executorService", executor);

        when(client.getConnector(anyString())).thenAnswer(i -> info(i.getArgument(0)));
        when(client.getConnectorStatus(anyString())).thenAnswer(i -> status(i.getArgument(0)));
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void onlyRequestedPageIsFetchedAfterSearchAndAclFiltering() throws Exception {
        List<String> matching = IntStream.rangeClosed(1, 12)
            .mapToObj(i -> String.format("match-%02d", i))
            .collect(Collectors.toList());
        List<String> all = new ArrayList<>(matching);
        all.add("other-1");
        all.add("match-denied");
        Collections.shuffle(all, new Random(42));
        when(client.getConnectorNames()).thenReturn(all);

        PagedList<ConnectDefinition> page2 = repository.getPaginatedDefinitions(
            CLUSTER, CONNECT, pagination(2), Optional.of("match"), Optional.empty(), List.of("match-\\d+")
        );

        List<String> expectedPage = matching.subList(PAGE_SIZE, 2 * PAGE_SIZE);
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
    void lastPageIsFetchedWithoutOtherPages() throws Exception {
        List<String> names = IntStream.rangeClosed(1, 12)
            .mapToObj(i -> String.format("c-%02d", i))
            .collect(Collectors.toList());
        when(client.getConnectorNames()).thenReturn(new ArrayList<>(names));

        PagedList<ConnectDefinition> page3 = repository.getPaginatedDefinitions(
            CLUSTER, CONNECT, pagination(3), Optional.empty(), Optional.empty(), List.of()
        );

        assertEquals(List.of("c-11", "c-12"), page3.stream().map(ConnectDefinition::getName).collect(Collectors.toList()));
        assertEquals(12, page3.total());
        verify(client, times(2)).getConnector(anyString());
        verify(client, never()).getConnectorsStatus();
    }

    @Test
    void statusFilterUsesStatusListingAndKeepsFullFilteredTotal() throws Exception {
        List<String> names = IntStream.rangeClosed(1, 12)
            .mapToObj(i -> String.format("c-%02d", i))
            .collect(Collectors.toList());
        when(client.getConnectorNames()).thenReturn(new ArrayList<>(names));

        Map<String, ConnectorExpanded> statuses = new HashMap<>();
        names.forEach(name -> {
            ConnectorExpanded expanded = new ConnectorExpanded();
            expanded.setStatus(status(name, Integer.parseInt(name.substring(2)) % 2 == 0 ? "FAILED" : "RUNNING"));
            statuses.put(name, expanded);
        });
        when(client.getConnectorsStatus()).thenReturn(statuses);

        PagedList<ConnectDefinition> page1 = repository.getPaginatedDefinitions(
            CLUSTER, CONNECT, pagination(1), Optional.empty(), Optional.of("failed"), List.of()
        );

        assertEquals(List.of("c-02", "c-04", "c-06", "c-08", "c-10"),
            page1.stream().map(ConnectDefinition::getName).collect(Collectors.toList()));
        assertEquals(6, page1.total());
        verify(client, times(1)).getConnectorsStatus();
        verify(client, times(PAGE_SIZE)).getConnector(anyString());
        verify(client, never()).getConnector("c-12");
    }

    private Pagination pagination(int page) {
        return new Pagination(PAGE_SIZE, URIBuilder.empty(), page);
    }

    private void inject(String field, Object value) throws Exception {
        Field f = ConnectRepository.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(repository, value);
    }

    private static ConnectorInfo info(String name) {
        ConnectorInfo info = new ConnectorInfo();
        info.setName(name);
        info.setType("sink");
        info.setConfig(Map.of());
        info.setTasks(List.of());
        return info;
    }

    private static ConnectorStatus status(String name) {
        return status(name, "RUNNING");
    }

    private static ConnectorStatus status(String name, String taskState) {
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
}
