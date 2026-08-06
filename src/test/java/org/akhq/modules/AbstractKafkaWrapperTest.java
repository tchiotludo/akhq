package org.akhq.modules;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractKafkaWrapperTest {
    private static final String CLUSTER_ID = "cluster-a";

    @Mock
    private KafkaModule kafkaModule;

    @Mock
    private AdminClient adminClient;

    @Mock
    private DescribeTopicsResult describeTopicsResult;

    private TestKafkaWrapper kafkaWrapper;

    @BeforeEach
    void beforeEach() throws Exception {
        kafkaWrapper = new TestKafkaWrapper();

        Field kafkaModuleField = AbstractKafkaWrapper.class.getDeclaredField("kafkaModule");
        kafkaModuleField.setAccessible(true);
        kafkaModuleField.set(kafkaWrapper, kafkaModule);
    }

    @Test
    void describeExistingTopicsOmitsUnknownTopicFromSingleBatch() throws Exception {
        KafkaFuture<TopicDescription> existingFuture = successfulFuture(
            new TopicDescription("topic-existing", false, List.of())
        );
        KafkaFuture<TopicDescription> missingFuture = failedFuture(
            new UnknownTopicOrPartitionException("missing")
        );

        when(kafkaModule.getAdminClient(CLUSTER_ID)).thenReturn(adminClient);
        when(adminClient.describeTopics(List.of("topic-existing", "topic-missing"))).thenReturn(describeTopicsResult);
        when(describeTopicsResult.topicNameValues()).thenReturn(Map.of(
            "topic-existing", existingFuture,
            "topic-missing", missingFuture
        ));

        Map<String, TopicDescription> descriptions = kafkaWrapper.describeExistingTopics(CLUSTER_ID, List.of(
            "topic-existing",
            "topic-missing"
        ));

        assertEquals(List.of("topic-existing"), descriptions.keySet().stream().toList());
        verify(adminClient).describeTopics(List.of("topic-existing", "topic-missing"));
    }

    @Test
    void describeExistingTopicsRefreshesPreviouslyCachedMissingTopic() throws Exception {
        DescribeTopicsResult firstDescribeTopicsResult = mock(DescribeTopicsResult.class);
        DescribeTopicsResult secondDescribeTopicsResult = mock(DescribeTopicsResult.class);

        KafkaFuture<TopicDescription> existingFuture = successfulFuture(
            new TopicDescription("topic-a", false, List.of())
        );
        KafkaFuture<TopicDescription> missingFuture = failedFuture(
            new UnknownTopicOrPartitionException("missing")
        );

        when(kafkaModule.getAdminClient(CLUSTER_ID)).thenReturn(adminClient);
        when(adminClient.describeTopics(List.of("topic-a"))).thenReturn(firstDescribeTopicsResult, secondDescribeTopicsResult);
        when(firstDescribeTopicsResult.topicNameValues()).thenReturn(Map.of(
            "topic-a", existingFuture
        ));
        when(secondDescribeTopicsResult.topicNameValues()).thenReturn(Map.of(
            "topic-a", missingFuture
        ));

        Map<String, TopicDescription> firstDescriptions = kafkaWrapper.describeExistingTopics(CLUSTER_ID, List.of("topic-a"));
        Map<String, TopicDescription> secondDescriptions = kafkaWrapper.describeExistingTopics(CLUSTER_ID, List.of("topic-a"));

        assertEquals(Set.of("topic-a"), firstDescriptions.keySet());
        assertTrue(secondDescriptions.isEmpty());
        verify(adminClient, times(2)).describeTopics(List.of("topic-a"));
    }

    @Test
    void describeExistingTopicsDoesNotSwallowUnrelatedKafkaErrors() throws Exception {
        KafkaFuture<TopicDescription> timeoutFuture = failedFuture(new TimeoutException("timeout"));

        when(kafkaModule.getAdminClient(CLUSTER_ID)).thenReturn(adminClient);
        when(adminClient.describeTopics(List.of("topic-a"))).thenReturn(describeTopicsResult);
        when(describeTopicsResult.topicNameValues()).thenReturn(Map.of(
            "topic-a", timeoutFuture
        ));

        ExecutionException exception = assertThrows(
            ExecutionException.class,
            () -> kafkaWrapper.describeExistingTopics(CLUSTER_ID, List.of("topic-a"))
        );

        assertInstanceOf(TimeoutException.class, exception.getCause());
    }

    @Test
    void describeExistingTopicsOnlyOmitsUnknownTopicFromExecutionExceptionCause() throws Exception {
        KafkaFuture<TopicDescription> future = mockFuture();
        when(future.get()).thenThrow(new UnknownTopicOrPartitionException("missing"));

        when(kafkaModule.getAdminClient(CLUSTER_ID)).thenReturn(adminClient);
        when(adminClient.describeTopics(List.of("topic-a"))).thenReturn(describeTopicsResult);
        when(describeTopicsResult.topicNameValues()).thenReturn(Map.of(
            "topic-a", future
        ));

        assertThrows(
            UnknownTopicOrPartitionException.class,
            () -> kafkaWrapper.describeExistingTopics(CLUSTER_ID, List.of("topic-a"))
        );
    }

    @Test
    void describeExistingTopicsRefreshesRequestedCacheEntriesWithoutClearingUnrelatedTopics() throws Exception {
        DescribeTopicsResult initialDescribeTopicsResult = mock(DescribeTopicsResult.class);
        DescribeTopicsResult refreshDescribeTopicsResult = mock(DescribeTopicsResult.class);

        KafkaFuture<TopicDescription> initialTopicAFuture = successfulFuture(
            new TopicDescription("topic-a", false, List.of())
        );
        KafkaFuture<TopicDescription> unrelatedTopicFuture = successfulFuture(
            new TopicDescription("unrelated-topic", false, List.of())
        );
        KafkaFuture<TopicDescription> missingTopicAFuture = failedFuture(
            new UnknownTopicOrPartitionException("missing")
        );
        KafkaFuture<TopicDescription> topicBFuture = successfulFuture(
            new TopicDescription("topic-b", false, List.of())
        );

        when(kafkaModule.getAdminClient(CLUSTER_ID)).thenReturn(adminClient);
        when(adminClient.describeTopics(List.of("topic-a", "unrelated-topic"))).thenReturn(initialDescribeTopicsResult);
        when(initialDescribeTopicsResult.topicNameValues()).thenReturn(Map.of(
            "topic-a", initialTopicAFuture,
            "unrelated-topic", unrelatedTopicFuture
        ));

        kafkaWrapper.describeExistingTopics(CLUSTER_ID, List.of("topic-a", "unrelated-topic"));

        when(adminClient.describeTopics(List.of("topic-a", "topic-b"))).thenReturn(refreshDescribeTopicsResult);
        when(refreshDescribeTopicsResult.topicNameValues()).thenReturn(Map.of(
            "topic-a", missingTopicAFuture,
            "topic-b", topicBFuture
        ));

        Map<String, TopicDescription> descriptions = kafkaWrapper.describeExistingTopics(CLUSTER_ID, List.of("topic-a", "topic-b"));
        Map<String, TopicDescription> unrelatedDescription = kafkaWrapper.describeTopics(CLUSTER_ID, List.of("unrelated-topic"));

        assertEquals(Set.of("topic-b"), descriptions.keySet());
        assertEquals(Set.of("unrelated-topic"), unrelatedDescription.keySet());
        verify(adminClient).describeTopics(List.of("topic-a", "unrelated-topic"));
        verify(adminClient).describeTopics(List.of("topic-a", "topic-b"));
        verify(adminClient, never()).describeTopics(List.of("unrelated-topic"));
    }

    private KafkaFuture<TopicDescription> successfulFuture(TopicDescription description) throws ExecutionException, InterruptedException {
        KafkaFuture<TopicDescription> future = mockFuture();
        when(future.get()).thenReturn(description);
        return future;
    }

    private KafkaFuture<TopicDescription> failedFuture(Throwable throwable) throws ExecutionException, InterruptedException {
        KafkaFuture<TopicDescription> future = mockFuture();
        when(future.get()).thenThrow(new ExecutionException(throwable));
        return future;
    }

    @SuppressWarnings("unchecked")
    private KafkaFuture<TopicDescription> mockFuture() {
        return mock(KafkaFuture.class);
    }

    private static class TestKafkaWrapper extends AbstractKafkaWrapper {
    }
}
