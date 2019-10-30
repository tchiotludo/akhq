package org.kafkahq.repositories;

import org.apache.kafka.common.resource.ResourceType;
import org.junit.jupiter.api.Test;
import org.kafkahq.AbstractTest;
import org.kafkahq.KafkaTestCluster;
import org.kafkahq.models.User;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserRepositoryTest extends AbstractTest {

    @Inject
    private UserRepository userRepository;

    @Test
    public void findAll() {
        assertEquals(2,
                userRepository.findAll(KafkaTestCluster.CLUSTER_ID, Optional.empty()).size());
    }

    @Test
    public void findAll_with_filter() {
        var searchResult = userRepository.findAll(KafkaTestCluster.CLUSTER_ID, Optional.of("myuser"));
        assertEquals(1, searchResult.size());
        assertEquals("user:myuser", searchResult.get(0).getName());
    }

    @Test
    public void findByUser_all() {
        var searchResult = userRepository.findByUser(KafkaTestCluster.CLUSTER_ID, User.encodeUsername("user:myuser"), Optional.empty());
        assertEquals("user:myuser", searchResult.getName());
        assertEquals(2, searchResult.getAcls().entrySet().size());
        assertEquals(5,
                searchResult.getAcls().entrySet().stream().mapToInt(
                        entry -> entry.getValue().entrySet()
                                .stream()
                                .mapToInt(entry2 -> entry2.getValue().size()).sum()
                ).sum())      ;
    }

    @Test
    public void findByUser_for_topic() {
        var searchResult = userRepository.findByUser(KafkaTestCluster.CLUSTER_ID, User.encodeUsername("user:myuser"), Optional.of("topic"));
        assertEquals("user:myuser", searchResult.getName());
        assertEquals(1, searchResult.getAcls().entrySet().size());
        assertEquals(3,
                searchResult.getAcls().get("topic")
                        .entrySet().stream().mapToInt(entry -> entry.getValue().size()).sum());
    }

    @Test
    public void findByUser_for_group() {
        var searchResult = userRepository.findByUser(KafkaTestCluster.CLUSTER_ID, User.encodeUsername("user:myuser"), Optional.of("group"));
        assertEquals("user:myuser", searchResult.getName());
        assertEquals(1, searchResult.getAcls().entrySet().size());
        assertEquals(2,
                searchResult.getAcls().get("group")
                        .entrySet().stream().mapToInt(entry -> entry.getValue().size()).sum());
    }

    @Test
    public void findByResourceType_topic() {
        List<User> searchResult = userRepository.findByResourceType(KafkaTestCluster.CLUSTER_ID, ResourceType.TOPIC, "testAclTopic");
        assertEquals(2, searchResult.size());
        assertEquals(3,
                searchResult.stream().mapToInt(
                        user -> user.getAcls().entrySet().stream().mapToInt(
                                entry -> entry.getValue().entrySet()
                                        .stream()
                                        .mapToInt(entry2 -> entry2.getValue().size()).sum()
                        ).sum()).sum()
                );
    }

    @Test
    public void findByResourceType_group() {
        List<User> searchResult = userRepository.findByResourceType(KafkaTestCluster.CLUSTER_ID, ResourceType.GROUP, "groupConsumer");
        assertEquals(2, searchResult.size());
        assertEquals(2,
                searchResult.stream().mapToInt(
                        user -> user.getAcls().entrySet().stream().mapToInt(
                                entry -> entry.getValue().entrySet()
                                        .stream()
                                        .mapToInt(entry2 -> entry2.getValue().size()).sum()
                        ).sum()).sum()
        );
    }


}
