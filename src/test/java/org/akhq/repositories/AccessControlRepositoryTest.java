package org.akhq.repositories;

import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.AccessControl;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccessControlRepositoryTest extends AbstractTest {
    @Inject
    private AccessControlListRepository aclRepository;

    @Test
    void findAll() throws ExecutionException, InterruptedException {
        List<AccessControl> all = aclRepository.findAll(KafkaTestCluster.CLUSTER_ID, Optional.empty());
        assertEquals(2, all.size());
    }

    @Test
    void findAllWithFilter() throws ExecutionException, InterruptedException {
        var searchResult = aclRepository.findAll(KafkaTestCluster.CLUSTER_ID, Optional.of("toto"));
        assertEquals(1, searchResult.size());
        assertEquals("user:toto", searchResult.get(0).getPrincipal());
    }

    @Test
    void findAllByUser() throws ExecutionException, InterruptedException {
        var searchResult = aclRepository.findByPrincipal(KafkaTestCluster.CLUSTER_ID, AccessControl.encodePrincipal("user:toto"), Optional.empty());
        assertEquals("user:toto", searchResult.getPrincipal());
        assertEquals(5, searchResult.getAcls().size());
        assertEquals(1, searchResult
            .getAcls()
            .stream()
            .filter(acl -> acl.getOperation().getPermissionType() == AclPermissionType.DENY)
            .count()
        );
    }

    @Test
    void findAllBySecondUser() throws ExecutionException, InterruptedException {
        var searchResult = aclRepository.findByPrincipal(KafkaTestCluster.CLUSTER_ID, AccessControl.encodePrincipal("test:toto"), Optional.empty());
        assertEquals("test:toto", searchResult.getPrincipal());
        assertEquals(2, searchResult.getAcls().size());
        assertEquals(2, searchResult
            .getAcls()
            .stream()
            .filter(acl -> acl.getOperation().getPermissionType() == AclPermissionType.ALLOW)
            .count()
        );
    }

    @Test
    void findHostByUser() throws ExecutionException, InterruptedException {
        var searchResult = aclRepository.findByPrincipal(KafkaTestCluster.CLUSTER_ID, AccessControl.encodePrincipal("user:tata"), Optional.empty());
        assertEquals("user:tata", searchResult.getPrincipal());
        assertEquals(2, searchResult.getAcls().size());
        assertEquals(1, searchResult
            .getAcls()
            .stream()
            .filter(acl -> acl.getHost().equals("*"))
            .count()
        );
        assertEquals(1, searchResult
            .getAcls()
            .stream()
            .filter(acl -> acl.getHost().equals("my-host"))
            .count()
        );
    }

    @Test
    void findByUserForTopic() throws ExecutionException, InterruptedException {
        var searchResult = aclRepository.findByPrincipal(KafkaTestCluster.CLUSTER_ID, AccessControl.encodePrincipal("user:toto"), Optional.of(ResourceType.TOPIC));
        assertEquals("user:toto", searchResult.getPrincipal());
        assertEquals(3, searchResult.getAcls().size());
    }

    @Test
    void findByUserForGroup() throws ExecutionException, InterruptedException {
        var searchResult = aclRepository.findByPrincipal(KafkaTestCluster.CLUSTER_ID, AccessControl.encodePrincipal("user:toto"), Optional.of(ResourceType.GROUP));
        assertEquals("user:toto", searchResult.getPrincipal());
        assertEquals(2, searchResult.getAcls().size());
    }

    @Test
    void findByResourceTypeTopic() throws ExecutionException, InterruptedException {
        List<AccessControl> searchResult = aclRepository.findByResourceType(KafkaTestCluster.CLUSTER_ID, ResourceType.TOPIC, "testAclTopic");
        assertEquals(2, searchResult.size());
        assertEquals(2, searchResult.stream().mapToLong(r -> r.getAcls().size()).sum());
    }

    @Test
    void findByResourceTypeGroup() throws ExecutionException, InterruptedException {
        List<AccessControl> searchResult = aclRepository.findByResourceType(KafkaTestCluster.CLUSTER_ID, ResourceType.GROUP, "groupConsumer");
        assertEquals(2, searchResult.size());
        assertEquals(2, searchResult.stream().mapToLong(r -> r.getAcls().size()).sum());
    }
}
