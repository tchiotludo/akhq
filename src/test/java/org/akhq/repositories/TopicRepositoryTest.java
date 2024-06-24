package org.akhq.repositories;

import io.micronaut.context.ApplicationContext;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.ServerAuthentication;
import io.micronaut.security.utils.DefaultSecurityService;
import io.micronaut.security.utils.SecurityService;
import jakarta.inject.Inject;
import org.akhq.AbstractTest;
import org.akhq.KafkaClusterExtension;
import org.akhq.KafkaTestCluster;
import org.akhq.models.Config;
import org.akhq.models.Partition;
import org.akhq.models.Topic;
import org.akhq.utils.Pagination;
import org.apache.kafka.common.config.TopicConfig;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(KafkaClusterExtension.class)
class TopicRepositoryTest extends AbstractTest {

    @Inject
    @InjectMocks
    protected TopicRepository topicRepository;

    @Inject
    protected ConfigRepository configRepository;

    @Mock
    ApplicationContext applicationContext;

    private final FilterGenerated filterGenerated = new FilterGenerated();

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void list() throws ExecutionException, InterruptedException {
        assertEquals(KafkaTestCluster.TOPIC_ALL_COUNT, topicRepository.list(
            KafkaTestCluster.CLUSTER_ID,
            new Pagination(100, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.ALL,
            Optional.empty(),
            List.of()
        ).stream().filter(filterGenerated).toList().size());
    }

    @Test
    void listNoInternal() throws ExecutionException, InterruptedException {
        assertEquals(KafkaTestCluster.TOPIC_HIDE_INTERNAL_COUNT, topicRepository.list(
            KafkaTestCluster.CLUSTER_ID,
            new Pagination(100, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.HIDE_INTERNAL,
            Optional.empty(),
            List.of()
        ).stream().filter(filterGenerated).toList().size());
    }

    @Test
    void listNoInternalStream() throws ExecutionException, InterruptedException {
        assertEquals(KafkaTestCluster.TOPIC_HIDE_INTERNAL_STREAM_COUNT, topicRepository.list(
            KafkaTestCluster.CLUSTER_ID,
            new Pagination(100, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.HIDE_INTERNAL_STREAM,
            Optional.empty(),
            List.of()
        ).stream().filter(filterGenerated).toList().size());
    }

    @Test
    void listNoStream() throws ExecutionException, InterruptedException {
        assertEquals(KafkaTestCluster.TOPIC_HIDE_STREAM_COUNT, topicRepository.list(
            KafkaTestCluster.CLUSTER_ID,
            new Pagination(100, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.HIDE_STREAM,
            Optional.empty(),
            List.of()
        ).stream().filter(filterGenerated).toList().size());
    }

    @Test
    void listWithTopicRegex() throws ExecutionException, InterruptedException {
        mockApplicationContext();
        assertEquals(1, topicRepository.list(
            KafkaTestCluster.CLUSTER_ID,
            new Pagination(100, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.ALL,
            Optional.empty(),
            List.of("rando.*")
        ).size());
    }

    @Test
    void search() throws ExecutionException, InterruptedException {
        assertEquals(1, topicRepository.list(
            KafkaTestCluster.CLUSTER_ID,
            new Pagination(100, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.ALL,
            Optional.of("ra do"),
            List.of()
        ).size());
    }

    @Test
    void searchWithTopicRegex() throws ExecutionException, InterruptedException {
        mockApplicationContext();
        assertEquals(0, topicRepository.list(
            KafkaTestCluster.CLUSTER_ID,
            new Pagination(100, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.ALL,
            Optional.of("stream"),
            List.of("rando.*")
        ).size());
    }

    @Test
    void create() throws ExecutionException, InterruptedException {
        topicRepository.create(KafkaTestCluster.CLUSTER_ID, "createEmptyConfig", 8, (short) 1, Collections.emptyList()
        );

        assertEquals(8, topicRepository.findByName(KafkaTestCluster.CLUSTER_ID, "createEmptyConfig").getPartitions().size());

        topicRepository.delete(KafkaTestCluster.CLUSTER_ID, "createEmptyConfig");
    }

    @Test
    void createWithConfig() throws ExecutionException, InterruptedException {
        topicRepository.create(KafkaTestCluster.CLUSTER_ID, "createWithConfig", 8, (short) 1, Collections.singletonList(
            new Config(TopicConfig.SEGMENT_MS_CONFIG, "1000")
        ));

        Optional<String> option = configRepository.findByTopic(KafkaTestCluster.CLUSTER_ID, "createWithConfig")
            .stream()
            .filter(r -> r.getName().equals(TopicConfig.SEGMENT_MS_CONFIG))
            .findFirst()
            .map(Config::getValue);

        assertEquals(8, topicRepository.findByName(KafkaTestCluster.CLUSTER_ID, "createWithConfig").getPartitions().size());
        assertEquals("1000", option.get());

        topicRepository.delete(KafkaTestCluster.CLUSTER_ID, "createWithConfig");
    }

    @Test
    void offset() throws ExecutionException, InterruptedException {
        Optional<Partition> compacted = topicRepository
            .findByName(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED)
            .getPartitions()
            .stream()
            .filter(partition -> partition.getId() == 0)
            .findFirst();

        assertTrue(compacted.isPresent());
        // 50 messages with the same key (1 remaining after compaction) : 1st offset is 49
        assertEquals(49, compacted.get().getFirstOffset());
        // 50 messages with the same key + 50 random messages : last offset is 100
        assertEquals(100, compacted.get().getLastOffset());
    }

    @Test
    void partition() throws ExecutionException, InterruptedException {
        assertEquals(3, topicRepository.findByName(KafkaTestCluster.CLUSTER_ID, KafkaTestCluster.TOPIC_COMPACTED).getPartitions().size());
    }

    @Test
    void increasePartition() throws ExecutionException, InterruptedException {
        topicRepository.create(KafkaTestCluster.CLUSTER_ID, "increasePartition", 8, (short) 1, Collections.emptyList()
        );
        topicRepository.increasePartition(KafkaTestCluster.CLUSTER_ID, "increasePartition", 9);

        assertEquals(9, topicRepository.findByName(KafkaTestCluster.CLUSTER_ID, "increasePartition").getPartitions().size());

        topicRepository.delete(KafkaTestCluster.CLUSTER_ID, "increasePartition");
    }

    private void mockApplicationContext() {
        Authentication auth = new ServerAuthentication("test", List.of(), Map.of());
        DefaultSecurityService securityService = Mockito.mock(DefaultSecurityService.class);
        when(securityService.getAuthentication()).thenReturn(Optional.of(auth));
        when(applicationContext.containsBean(SecurityService.class)).thenReturn(true);
        when(applicationContext.getBean(SecurityService.class)).thenReturn(securityService);
    }

    private static class FilterGenerated implements Predicate<Topic> {
        private final static String PATTERN = "generated_";

        @Override
        public boolean test(Topic topic) {
            return !topic.getName().startsWith(PATTERN);
        }
    }
}
