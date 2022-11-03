package org.akhq.repositories;

import io.micronaut.context.ApplicationContext;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.ServerAuthentication;
import io.micronaut.security.utils.DefaultSecurityService;
import io.micronaut.security.utils.SecurityService;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.utils.Pagination;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import jakarta.inject.Inject;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class ConsumerGroupRepositoryTest extends AbstractTest {

    @Inject
    @InjectMocks
    protected ConsumerGroupRepository consumerGroupRepository;

    @Mock
    ApplicationContext applicationContext;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void list() throws ExecutionException, InterruptedException {
        mockApplicationContext();
        assertEquals(KafkaTestCluster.CONSUMER_GROUP_COUNT, consumerGroupRepository.list(
            KafkaTestCluster.CLUSTER_ID,
            new Pagination(100, URIBuilder.empty(), 1),
            Optional.empty()
        ).size());
    }

    @Test
    void listWithConsumerGroupRegex() throws ExecutionException, InterruptedException {
        mockApplicationContext();
        assertEquals(KafkaTestCluster.CONSUMER_GROUP_COUNT, consumerGroupRepository.list(
            KafkaTestCluster.CLUSTER_ID,
            new Pagination(100, URIBuilder.empty(), 1),
            Optional.empty()
        ).size());
    }

    @Test
    void search() throws ExecutionException, InterruptedException {
        assertEquals(1, consumerGroupRepository.list(
            KafkaTestCluster.CLUSTER_ID,
            new Pagination(100, URIBuilder.empty(), 1),
            Optional.of("consu 2")
        ).size());
    }

    @Test
    void searchWithTopicRegex() throws ExecutionException, InterruptedException {
        mockApplicationContext();
        assertEquals(0, consumerGroupRepository.list(
            KafkaTestCluster.CLUSTER_ID,
            new Pagination(100, URIBuilder.empty(), 1),
            Optional.of("stream")
        ).size());
    }

    @Test
    void findByNameWithTopicRegex() throws ExecutionException, InterruptedException {
        mockApplicationContext();
        assertThrows(NoSuchElementException.class, () -> {
            consumerGroupRepository.findByName(KafkaTestCluster.CLUSTER_ID, "cgroup-1");
        });

        assertEquals(1, consumerGroupRepository.findByName(KafkaTestCluster.CLUSTER_ID, List.of("consumer-6", "cgroup-1")).size());
    }

    private void mockApplicationContext() {
        Authentication auth = new ServerAuthentication("test", List.of(), Collections.singletonMap("consumerGroupsFilterRegexp", new ArrayList<>(Arrays.asList("consumer-.*"))));
        DefaultSecurityService securityService = Mockito.mock(DefaultSecurityService.class);
        when(securityService.getAuthentication()).thenReturn(Optional.of(auth));
        when(applicationContext.containsBean(SecurityService.class)).thenReturn(true);
        when(applicationContext.getBean(SecurityService.class)).thenReturn(securityService);
    }
}
