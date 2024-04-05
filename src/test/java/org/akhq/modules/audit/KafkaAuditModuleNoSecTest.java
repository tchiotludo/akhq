package org.akhq.modules.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.akhq.KafkaClusterExtension;
import org.akhq.KafkaTestCluster;
import org.akhq.repositories.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

/**
 * This only tests instantiation
 */
@ExtendWith(KafkaClusterExtension.class)
@MicronautTest(environments = "audit-no-security")
public class KafkaAuditModuleNoSecTest {

    @Inject
    @InjectMocks
    protected TopicRepository topicRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void topicAudit() throws ExecutionException, InterruptedException, IOException {
        topicRepository.create(KafkaTestCluster.CLUSTER_ID, "dummy-no-sec", 1, (short) 1, Collections.emptyList()
        );

    }

}
