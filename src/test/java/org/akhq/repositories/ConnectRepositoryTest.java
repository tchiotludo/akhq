package org.akhq.repositories;

import io.micronaut.context.ApplicationContext;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.DefaultAuthentication;
import io.micronaut.security.utils.DefaultSecurityService;
import io.micronaut.security.utils.SecurityService;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.ConnectDefinition;
import org.akhq.models.ConnectPlugin;
import org.akhq.utils.Pagination;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import scala.None;

import javax.inject.Inject;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Slf4j
public class ConnectRepositoryTest extends AbstractTest {
    
    @Inject
    @InjectMocks
    private ConnectRepository repository;

    @Mock
    ApplicationContext applicationContext;

    @BeforeEach
    public void before(){
        MockitoAnnotations.initMocks(this);
    }

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

    @AfterEach
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
                "topics", KafkaTestCluster.TOPIC_CONNECT
            )
        );

        repository.create(
            KafkaTestCluster.CLUSTER_ID,
            "connect-2",
            "ConnectRepositoryTest2",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", path1,
                "topics", KafkaTestCluster.TOPIC_CONNECT
            )
        );


        List<ConnectDefinition> all1 = repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-1", Optional.empty());
        assertEquals(1, all1.size());

        List<ConnectDefinition> all2 = repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-2", Optional.empty());
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
                "topics", KafkaTestCluster.TOPIC_CONNECT
            )
        );

        repository.update(
            KafkaTestCluster.CLUSTER_ID,
            "connect-2",
            "ConnectRepositoryTest2",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", path2,
                "topics", KafkaTestCluster.TOPIC_CONNECT
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
        assertEquals(0, repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-1", Optional.empty()).size());
        assertEquals(0, repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-2", Optional.empty()).size());
    }

    private void mockApplicationContext() {
        Authentication auth = new DefaultAuthentication("test", Collections.singletonMap("connectsFilterRegexp", new ArrayList<>(Arrays.asList("^prefixed.*$"))));
        DefaultSecurityService securityService = Mockito.mock(DefaultSecurityService.class);
        when(securityService.getAuthentication()).thenReturn(Optional.of(auth));
        when(applicationContext.containsBean(SecurityService.class)).thenReturn(true);
        when(applicationContext.getBean(SecurityService.class)).thenReturn(securityService);
    }

    @Test
    public void getFilteredList() {

        repository.create(
            KafkaTestCluster.CLUSTER_ID,
            "connect-1",
            "prefixed.Matching1",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", "/tmp/test.txt",
                "topics", KafkaTestCluster.TOPIC_CONNECT
            )
        );

        repository.create(
            KafkaTestCluster.CLUSTER_ID,
            "connect-1",
            "prefixed.Matching2",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", "/tmp/test.txt",
                "topics", KafkaTestCluster.TOPIC_CONNECT
            )
        );

        repository.create(
            KafkaTestCluster.CLUSTER_ID,
            "connect-1",
            "not.Matching3",
            ImmutableMap.of(
                "connector.class", "FileStreamSinkConnector",
                "file", "/tmp/test.txt",
                "topics", KafkaTestCluster.TOPIC_CONNECT
            )
        );
        
        mockApplicationContext();

        List<ConnectDefinition> filtered = repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-1", Optional.empty());
        assertEquals(2, filtered.size());
        repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-1","prefixed.Matching1");
        repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-1","prefixed.Matching2");
        repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-1","not.Matching3");
    }


    @Test
    public void getFilteredBySearchList() {

        repository.create(
                KafkaTestCluster.CLUSTER_ID,
                "connect-1",
                "prefixed.Matching1",
                ImmutableMap.of(
                        "connector.class", "FileStreamSinkConnector",
                        "file", "/tmp/test.txt",
                        "topics", KafkaTestCluster.TOPIC_CONNECT
                )
        );

        repository.create(
                KafkaTestCluster.CLUSTER_ID,
                "connect-1",
                "prefixed.Matching2",
                ImmutableMap.of(
                        "connector.class", "FileStreamSinkConnector",
                        "file", "/tmp/test.txt",
                        "topics", KafkaTestCluster.TOPIC_CONNECT
                )
        );

        mockApplicationContext();

        List<ConnectDefinition> notFiltered = repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-1", Optional.empty());
        assertEquals(2, notFiltered.size());
        List<ConnectDefinition> filtered = repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-1", Optional.of("prefixed.Matching1"));
        assertEquals(1, filtered.size());
        List<ConnectDefinition> filteredAll = repository.getDefinitions(KafkaTestCluster.CLUSTER_ID, "connect-1", Optional.of("prefixed.Matching"));
        assertEquals(2, filteredAll.size());

        repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-1","prefixed.Matching1");
        repository.delete(KafkaTestCluster.CLUSTER_ID, "connect-1","prefixed.Matching2");
    }

}
