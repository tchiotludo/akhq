package org.akhq.repositories;

import org.apache.kafka.common.resource.ResourceType;
import org.junit.jupiter.api.Test;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.AccessControlList;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AccessControlListRepositoryTest extends AbstractTest {

    @Inject
    private AccessControlListRepository aclRepository;

    @Test
    public void findAll() {
        assertEquals(2,
                aclRepository.findAll(KafkaTestCluster.CLUSTER_ID, Optional.empty()).size());
    }

    @Test
    public void findAllWithFilter() {
        var searchResult = aclRepository.findAll(KafkaTestCluster.CLUSTER_ID, Optional.of("toto"));
        assertEquals(1, searchResult.size());
        assertEquals("user:toto", searchResult.get(0).getPrincipal());
    }

    @Test
    public void findAllByUser() {
        var searchResult = aclRepository.findByPrincipal(KafkaTestCluster.CLUSTER_ID, AccessControlList.encodePrincipal("user:toto"), Optional.empty());
        assertEquals("user:toto", searchResult.getPrincipal());
        assertEquals(2, searchResult.getPermissions().entrySet().size());
        assertEquals(5,
                searchResult.getPermissions().entrySet().stream().mapToInt(
                        entry -> entry.getValue().entrySet()
                                .stream()
                                .mapToInt(entry2 -> entry2.getValue().size()).sum()
                ).sum())      ;
    }

    @Test
    public void findByUserForTopic() {
        var searchResult = aclRepository.findByPrincipal(KafkaTestCluster.CLUSTER_ID, AccessControlList.encodePrincipal("user:toto"), Optional.of("topic"));
        assertEquals("user:toto", searchResult.getPrincipal());
        assertEquals(1, searchResult.getPermissions().entrySet().size());
        assertEquals(3,
                searchResult.getPermissions().get("topic")
                        .entrySet().stream().mapToInt(entry -> entry.getValue().size()).sum());
    }

    @Test
    public void findByUserForGroup() {
        var searchResult = aclRepository.findByPrincipal(KafkaTestCluster.CLUSTER_ID, AccessControlList.encodePrincipal("user:toto"), Optional.of("group"));
        assertEquals("user:toto", searchResult.getPrincipal());
        assertEquals(1, searchResult.getPermissions().entrySet().size());
        assertEquals(2,
                searchResult.getPermissions().get("group")
                        .entrySet().stream().mapToInt(entry -> entry.getValue().size()).sum());
    }

    @Test
    public void findByResourceTypeTopic() {
        List<AccessControlList> searchResult = aclRepository.findByResourceType(KafkaTestCluster.CLUSTER_ID, ResourceType.TOPIC, "testAclTopic");
        assertEquals(2, searchResult.size());
        assertEquals(3,
                searchResult.stream().mapToInt(
                        acl -> acl.getPermissions().entrySet().stream().mapToInt(
                                entry -> entry.getValue().entrySet()
                                        .stream()
                                        .mapToInt(entry2 -> entry2.getValue().size()).sum()
                        ).sum()).sum()
                );
    }

    @Test
    public void findByResourceTypeGroup() {
        List<AccessControlList> searchResult = aclRepository.findByResourceType(KafkaTestCluster.CLUSTER_ID, ResourceType.GROUP, "groupConsumer");
        assertEquals(2, searchResult.size());
        assertEquals(2,
                searchResult.stream().mapToInt(
                        acl -> acl.getPermissions().entrySet().stream().mapToInt(
                                entry -> entry.getValue().entrySet()
                                        .stream()
                                        .mapToInt(entry2 -> entry2.getValue().size()).sum()
                        ).sum()).sum()
        );
    }


}
