package org.akhq.modules.audit;

import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.akhq.AbstractTest;
import org.akhq.KafkaClusterExtension;
import org.akhq.KafkaTestCluster;
import org.akhq.modules.KafkaModule;
import org.akhq.repositories.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

/**
 * This only tests instantiation
 */
@ExtendWith(KafkaClusterExtension.class)
@MicronautTest(environments = "audit-no-security")
public class KafkaAuditModuleNoSecTest extends AbstractTest  {

    @Inject
    @InjectMocks
    protected TopicRepository topicRepository;

    @Mock
    ApplicationContext applicationContext;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void topicAuditNoSec() throws ExecutionException, InterruptedException {
        topicRepository.create(KafkaTestCluster.CLUSTER_ID, "generated_dummy-no-sec", 1, (short) 1, Collections.emptyList()
        );
    }

}
