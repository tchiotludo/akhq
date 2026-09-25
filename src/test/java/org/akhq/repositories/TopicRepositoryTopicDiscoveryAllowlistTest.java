package org.akhq.repositories;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.errors.TimeoutException;
import org.akhq.configs.Connection;
import org.akhq.models.Partition;
import org.akhq.models.Topic;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.modules.KafkaModule;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicRepositoryTopicDiscoveryAllowlistTest {
    private static final String CLUSTER_A = "cluster-a";
    private static final String CLUSTER_B = "cluster-b";

    @Mock
    private AbstractKafkaWrapper kafkaWrapper;

    @Mock
    private KafkaModule kafkaModule;

    @Mock
    private LogDirRepository logDirRepository;

    private TopicRepository topicRepository;

    @BeforeEach
    void beforeEach() throws Exception {
        topicRepository = new TopicRepository();
        topicRepository.internalRegexps = List.of("^_.*$");
        topicRepository.streamRegexps = List.of("^.*-changelog$", "^.*-repartition$", "^.*-rekey$");

        setField("kafkaWrapper", kafkaWrapper);
        setField("kafkaModule", kafkaModule);
        setField("logDirRepository", logDirRepository);

        lenient().when(logDirRepository.findByTopic(anyString(), anyString())).thenReturn(Collections.emptyList());
    }

    @Test
    void absentTopicDiscoveryAllowlistUsesFullTopicDiscovery() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, null);
        mockListTopics(CLUSTER_A, "topic-a", "topic-b");
        mockTopicDetails(CLUSTER_A);

        PagedList<Topic> topics = topicRepository.list(
            CLUSTER_A,
            new Pagination(10, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.ALL,
            java.util.Optional.empty(),
            List.of()
        );

        assertEquals(List.of("topic-a", "topic-b"), names(topics));
        verify(kafkaWrapper).listTopics(CLUSTER_A);
        verify(kafkaWrapper, never()).describeExistingTopics(eq(CLUSTER_A), anyList());
    }

    @Test
    void emptyTopicDiscoveryAllowlistUsesFullTopicDiscovery() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, List.of());
        mockListTopics(CLUSTER_A, "topic-a");
        mockTopicDetails(CLUSTER_A);

        PagedList<Topic> topics = topicRepository.list(
            CLUSTER_A,
            new Pagination(10, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.ALL,
            java.util.Optional.empty(),
            List.of()
        );

        assertEquals(List.of("topic-a"), names(topics));
        verify(kafkaWrapper).listTopics(CLUSTER_A);
        verify(kafkaWrapper, never()).describeExistingTopics(eq(CLUSTER_A), anyList());
    }

    @Test
    void blankTopicDiscoveryAllowlistUsesFullTopicDiscovery() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, List.of("", "   "));
        mockListTopics(CLUSTER_A, "topic-a");
        mockTopicDetails(CLUSTER_A);

        PagedList<Topic> topics = topicRepository.list(
            CLUSTER_A,
            new Pagination(10, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.ALL,
            java.util.Optional.empty(),
            List.of()
        );

        assertEquals(List.of("topic-a"), names(topics));
        verify(kafkaWrapper).listTopics(CLUSTER_A);
        verify(kafkaWrapper, never()).describeExistingTopics(eq(CLUSTER_A), anyList());
    }

    @Test
    void activeTopicDiscoveryAllowlistSkipsFullTopicDiscoveryAndSortsCandidates() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, List.of("topic-b", "topic-a", "topic-c"));
        mockTopicDetails(CLUSTER_A);

        PagedList<Topic> topics = topicRepository.list(
            CLUSTER_A,
            new Pagination(10, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.ALL,
            java.util.Optional.empty(),
            List.of()
        );

        assertEquals(List.of("topic-a", "topic-b", "topic-c"), names(topics));
        verify(kafkaWrapper, never()).listTopics(CLUSTER_A);
        verify(kafkaWrapper).describeExistingTopics(CLUSTER_A, List.of("topic-a", "topic-b", "topic-c"));
    }

    @Test
    void topicDiscoveryAllowlistNormalizesValues() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, Arrays.asList(" topic-a ", "topic-a", "", "   ", null, "topic-b"));
        mockTopicDetails(CLUSTER_A);

        PagedList<Topic> topics = topicRepository.list(
            CLUSTER_A,
            new Pagination(10, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.ALL,
            java.util.Optional.empty(),
            List.of()
        );

        assertEquals(List.of("topic-a", "topic-b"), names(topics));
        verify(kafkaWrapper, never()).listTopics(CLUSTER_A);
        verify(kafkaWrapper).describeExistingTopics(CLUSTER_A, List.of("topic-a", "topic-b"));
    }

    @Test
    void topicDiscoveryAllowlistIntersectsWithRbacPatterns() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, List.of("topic-a", "topic-b", "secret-topic"));
        mockTopicDetails(CLUSTER_A);

        PagedList<Topic> topics = topicRepository.list(
            CLUSTER_A,
            new Pagination(10, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.ALL,
            java.util.Optional.empty(),
            List.of("^topic-.*$")
        );

        assertEquals(List.of("topic-a", "topic-b"), names(topics));
        verify(kafkaWrapper, never()).listTopics(CLUSTER_A);
        verify(kafkaWrapper).describeExistingTopics(CLUSTER_A, List.of("topic-a", "topic-b"));
    }

    @Test
    void topicDiscoveryAllowlistIntersectsWithSearch() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, List.of("orders-created", "orders-paid", "customers-created"));
        mockTopicDetails(CLUSTER_A);

        PagedList<Topic> topics = topicRepository.list(
            CLUSTER_A,
            new Pagination(10, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.ALL,
            java.util.Optional.of("orders"),
            List.of()
        );

        assertEquals(List.of("orders-created", "orders-paid"), names(topics));
        verify(kafkaWrapper, never()).listTopics(CLUSTER_A);
        verify(kafkaWrapper).describeExistingTopics(CLUSTER_A, List.of("orders-created", "orders-paid"));
    }

    @Test
    void topicDiscoveryAllowlistStillAppliesTopicListViews() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, List.of("topic-a", "_consumer_offsets", "topic-changelog"));
        mockTopicDetails(CLUSTER_A);

        PagedList<Topic> topics = topicRepository.list(
            CLUSTER_A,
            new Pagination(10, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.HIDE_INTERNAL_STREAM,
            java.util.Optional.empty(),
            List.of()
        );

        assertEquals(List.of("topic-a"), names(topics));
        verify(kafkaWrapper, never()).listTopics(CLUSTER_A);
        verify(kafkaWrapper).describeExistingTopics(CLUSTER_A, List.of("topic-a"));
    }

    @Test
    void topicDiscoveryAllowlistIsIsolatedPerConnection() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, List.of("topic-a"));
        configureConnection(CLUSTER_B, null);
        mockListTopics(CLUSTER_B, "topic-b");
        mockTopicDetails(CLUSTER_A);
        mockTopicDetails(CLUSTER_B);

        PagedList<Topic> clusterATopics = topicRepository.list(
            CLUSTER_A,
            new Pagination(10, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.ALL,
            java.util.Optional.empty(),
            List.of()
        );
        PagedList<Topic> clusterBTopics = topicRepository.list(
            CLUSTER_B,
            new Pagination(10, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.ALL,
            java.util.Optional.empty(),
            List.of()
        );

        assertEquals(List.of("topic-a"), names(clusterATopics));
        assertEquals(List.of("topic-b"), names(clusterBTopics));
        verify(kafkaWrapper, never()).listTopics(CLUSTER_A);
        verify(kafkaWrapper).listTopics(CLUSTER_B);
    }

    @Test
    void topicDiscoveryAllowlistResolvesExistenceBeforePaginationAndOffsetsOnlySelectedPage() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, List.of("topic-a", "topic-b", "topic-c", "topic-d"));
        mockTopicDetails(CLUSTER_A);

        PagedList<Topic> topics = topicRepository.list(
            CLUSTER_A,
            new Pagination(2, URIBuilder.empty(), 2),
            TopicRepository.TopicListView.ALL,
            java.util.Optional.empty(),
            List.of()
        );

        assertEquals(List.of("topic-c", "topic-d"), names(topics));
        assertEquals(4, topics.total());
        verify(kafkaWrapper, never()).listTopics(CLUSTER_A);
        verify(kafkaWrapper).describeExistingTopics(CLUSTER_A, List.of("topic-a", "topic-b", "topic-c", "topic-d"));
        verify(kafkaWrapper).describeTopicsOffsets(CLUSTER_A, List.of("topic-c", "topic-d"));
    }

    @Test
    void missingAllowlistedTopicBeforePageBoundaryDoesNotShortenFirstPage() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, List.of("a-missing", "b-existing", "c-existing"));
        when(kafkaWrapper.describeExistingTopics(CLUSTER_A, List.of("a-missing", "b-existing", "c-existing"))).thenReturn(descriptions(List.of(
            "b-existing",
            "c-existing"
        )));
        when(kafkaWrapper.describeTopicsOffsets(eq(CLUSTER_A), anyList())).thenAnswer(invocation -> offsets(invocation.getArgument(1)));

        PagedList<Topic> topics = topicRepository.list(
            CLUSTER_A,
            new Pagination(2, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.ALL,
            java.util.Optional.empty(),
            List.of()
        );

        assertEquals(List.of("b-existing", "c-existing"), names(topics));
        assertEquals(2, topics.total());
        verify(kafkaWrapper, never()).listTopics(CLUSTER_A);
        verify(kafkaWrapper).describeExistingTopics(CLUSTER_A, List.of("a-missing", "b-existing", "c-existing"));
        verify(kafkaWrapper).describeTopicsOffsets(CLUSTER_A, List.of("b-existing", "c-existing"));
    }

    @Test
    void missingAllowlistedTopicBetweenPagesDoesNotSkipExistingTopics() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, List.of("e-existing", "d-existing", "c-existing", "b-missing", "a-existing"));
        when(kafkaWrapper.describeExistingTopics(CLUSTER_A, List.of("a-existing", "b-missing", "c-existing", "d-existing", "e-existing"))).thenReturn(descriptions(List.of(
            "a-existing",
            "c-existing",
            "d-existing",
            "e-existing"
        )));
        when(kafkaWrapper.describeTopicsOffsets(eq(CLUSTER_A), anyList())).thenAnswer(invocation -> offsets(invocation.getArgument(1)));

        PagedList<Topic> topics = topicRepository.list(
            CLUSTER_A,
            new Pagination(2, URIBuilder.empty(), 2),
            TopicRepository.TopicListView.ALL,
            java.util.Optional.empty(),
            List.of()
        );

        assertEquals(List.of("d-existing", "e-existing"), names(topics));
        assertEquals(4, topics.total());
        verify(kafkaWrapper, never()).listTopics(CLUSTER_A);
        verify(kafkaWrapper).describeExistingTopics(CLUSTER_A, List.of("a-existing", "b-missing", "c-existing", "d-existing", "e-existing"));
        verify(kafkaWrapper).describeTopicsOffsets(CLUSTER_A, List.of("d-existing", "e-existing"));
    }

    @Test
    void allConfiguredAllowlistedTopicsMissingReturnsEmptyPageWithoutOffsets() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, List.of("a-missing", "b-missing"));
        when(kafkaWrapper.describeExistingTopics(CLUSTER_A, List.of("a-missing", "b-missing"))).thenReturn(Collections.emptyMap());

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(TopicRepository.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            PagedList<Topic> topics = topicRepository.list(
                CLUSTER_A,
                new Pagination(2, URIBuilder.empty(), 1),
                TopicRepository.TopicListView.ALL,
                java.util.Optional.empty(),
                List.of()
            );

            assertEquals(List.of(), names(topics));
            assertEquals(0, topics.total());
            assertTrue(hasWarning(appender, "a-missing"));
            assertTrue(hasWarning(appender, "b-missing"));
        } finally {
            logger.detachAppender(appender);
        }

        verify(kafkaWrapper, never()).listTopics(CLUSTER_A);
        verify(kafkaWrapper).describeExistingTopics(CLUSTER_A, List.of("a-missing", "b-missing"));
        verify(kafkaWrapper, never()).describeTopicsOffsets(eq(CLUSTER_A), anyList());
        verify(logDirRepository, never()).findByTopic(eq(CLUSTER_A), anyString());
    }

    @Test
    void missingAllowlistedTopicIsOmittedAndWarnedWithoutFallback() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, List.of("topic-existing-a", "topic-missing", "topic-existing-b"));
        when(kafkaWrapper.describeExistingTopics(eq(CLUSTER_A), anyList())).thenReturn(descriptions(List.of(
            "topic-existing-a",
            "topic-existing-b"
        )));
        when(kafkaWrapper.describeTopicsOffsets(eq(CLUSTER_A), anyList())).thenAnswer(invocation -> offsets(invocation.getArgument(1)));

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(TopicRepository.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            PagedList<Topic> topics = topicRepository.list(
                CLUSTER_A,
                new Pagination(10, URIBuilder.empty(), 1),
                TopicRepository.TopicListView.ALL,
                java.util.Optional.empty(),
                List.of()
            );

            assertEquals(List.of("topic-existing-a", "topic-existing-b"), names(topics));
            assertTrue(appender.list.stream().anyMatch(event ->
                event.getLevel().equals(Level.WARN) &&
                    event.getFormattedMessage().contains(CLUSTER_A) &&
                    event.getFormattedMessage().contains("topic-missing")
            ));
        } finally {
            logger.detachAppender(appender);
        }

        verify(kafkaWrapper, never()).listTopics(CLUSTER_A);
        verify(kafkaWrapper).describeTopicsOffsets(CLUSTER_A, List.of("topic-existing-a", "topic-existing-b"));
    }

    @Test
    void unrelatedAllowlistDescribeExceptionIsNotSwallowed() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, List.of("topic-a"));
        when(kafkaWrapper.describeExistingTopics(eq(CLUSTER_A), anyList()))
            .thenThrow(new ExecutionException(new TimeoutException("timeout")));

        assertThrows(ExecutionException.class, () -> topicRepository.list(
            CLUSTER_A,
            new Pagination(10, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.ALL,
            java.util.Optional.empty(),
            List.of()
        ));

        verify(kafkaWrapper, never()).listTopics(CLUSTER_A);
    }

    @Test
    void directTopicLookupIsIndependentFromTopicDiscoveryAllowlist() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, List.of("topic-a"));
        mockTopicDetails(CLUSTER_A);

        Topic topic = topicRepository.findByName(CLUSTER_A, "outside-topic");

        assertEquals("outside-topic", topic.getName());
        verify(kafkaWrapper).describeTopics(CLUSTER_A, List.of("outside-topic"));
        verify(kafkaWrapper, never()).describeExistingTopics(eq(CLUSTER_A), anyList());
        verify(kafkaWrapper, never()).listTopics(CLUSTER_A);
    }

    @Test
    void allNamesWithTopicDiscoveryAllowlistOmitsMissingTopics() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, List.of("topic-existing-a", "topic-missing", "topic-existing-b"));
        when(kafkaWrapper.describeExistingTopics(eq(CLUSTER_A), anyList())).thenReturn(descriptions(List.of(
            "topic-existing-a",
            "topic-existing-b"
        )));

        List<String> topics = topicRepository.all(
            CLUSTER_A,
            TopicRepository.TopicListView.ALL,
            java.util.Optional.empty(),
            List.of()
        );

        assertEquals(List.of("topic-existing-a", "topic-existing-b"), topics);
        verify(kafkaWrapper, never()).listTopics(CLUSTER_A);
    }

    @Test
    void fullDiscoveryAllNamesUsesListTopicsWhenAllowlistAbsent() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, null);
        mockListTopics(CLUSTER_A, "topic-b", "topic-a");

        List<String> topics = topicRepository.all(
            CLUSTER_A,
            TopicRepository.TopicListView.ALL,
            java.util.Optional.empty(),
            List.of()
        );

        assertEquals(List.of("topic-a", "topic-b"), topics);
        verify(kafkaWrapper).listTopics(CLUSTER_A);
    }

    @Test
    void allowlistMetadataRequestsUseFilteredCandidatesAndSelectedPageOffsets() throws ExecutionException, InterruptedException {
        configureConnection(CLUSTER_A, List.of("topic-a", "topic-b", "topic-c", "topic-d", "topic-e", "other"));
        mockTopicDetails(CLUSTER_A);

        topicRepository.list(
            CLUSTER_A,
            new Pagination(2, URIBuilder.empty(), 1),
            TopicRepository.TopicListView.ALL,
            java.util.Optional.of("topic"),
            List.of()
        );

        ArgumentCaptor<List<String>> descriptionsCaptor = listCaptor();
        ArgumentCaptor<List<String>> offsetsCaptor = listCaptor();
        verify(kafkaWrapper).describeExistingTopics(eq(CLUSTER_A), descriptionsCaptor.capture());
        verify(kafkaWrapper).describeTopicsOffsets(eq(CLUSTER_A), offsetsCaptor.capture());

        assertEquals(List.of("topic-a", "topic-b", "topic-c", "topic-d", "topic-e"), descriptionsCaptor.getValue());
        assertEquals(List.of("topic-a", "topic-b"), offsetsCaptor.getValue());
        verify(kafkaWrapper, never()).listTopics(CLUSTER_A);
    }

    private void configureConnection(String clusterId, List<String> allowlist) {
        Connection connection = new Connection(clusterId);
        if (allowlist != null) {
            connection.getTopicDiscovery().setAllowlist(allowlist);
        }

        lenient().when(kafkaModule.getConnection(clusterId)).thenReturn(connection);
    }

    private void mockListTopics(String clusterId, String... topics) throws ExecutionException {
        List<TopicListing> listings = Arrays.stream(topics)
            .map(this::topicListing)
            .collect(Collectors.toList());

        when(kafkaWrapper.listTopics(clusterId)).thenReturn(listings);
    }

    private void mockTopicDetails(String clusterId) throws ExecutionException, InterruptedException {
        lenient().when(kafkaWrapper.describeExistingTopics(eq(clusterId), anyList())).thenAnswer(invocation -> descriptions(invocation.getArgument(1)));
        lenient().when(kafkaWrapper.describeTopics(eq(clusterId), anyList())).thenAnswer(invocation -> descriptions(invocation.getArgument(1)));
        lenient().when(kafkaWrapper.describeTopicsOffsets(eq(clusterId), anyList())).thenAnswer(invocation -> offsets(invocation.getArgument(1)));
    }

    private TopicListing topicListing(String name) {
        TopicListing listing = org.mockito.Mockito.mock(TopicListing.class);
        when(listing.name()).thenReturn(name);
        return listing;
    }

    private Map<String, TopicDescription> descriptions(List<String> topics) {
        Map<String, TopicDescription> descriptions = new LinkedHashMap<>();
        topics.forEach(topic -> descriptions.put(topic, new TopicDescription(topic, false, List.of())));
        return descriptions;
    }

    private Map<String, List<Partition.Offsets>> offsets(List<String> topics) {
        Map<String, List<Partition.Offsets>> offsets = new LinkedHashMap<>();
        topics.forEach(topic -> offsets.put(topic, List.of()));
        return offsets;
    }

    private List<String> names(List<Topic> topics) {
        return topics.stream()
            .map(Topic::getName)
            .collect(Collectors.toList());
    }

    private boolean hasWarning(ListAppender<ILoggingEvent> appender, String topic) {
        return appender.list.stream().anyMatch(event ->
            event.getLevel().equals(Level.WARN) &&
                event.getFormattedMessage().contains(CLUSTER_A) &&
                event.getFormattedMessage().contains(topic)
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<List<String>> listCaptor() {
        return ArgumentCaptor.forClass((Class) List.class);
    }

    private void setField(String name, Object value) throws Exception {
        Field field = TopicRepository.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(topicRepository, value);
    }
}
