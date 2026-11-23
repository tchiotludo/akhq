package org.akhq.controllers;

import io.micronaut.security.authentication.AuthorizationException;
import org.akhq.models.MetadataQuorum;
import org.akhq.repositories.MetadataQuorumRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NodeControllerQuorumTest {
    private final MetadataQuorumRepository repository = mock(MetadataQuorumRepository.class);

    private NodeController controller(boolean clusterAllowed, Set<String> nodeIds) {
        return new NodeController(null, null, null, repository) {
            @Override
            protected void checkIfClusterAllowed(String cluster) {
                if (!clusterAllowed) throw new AuthorizationException(null);
            }

            @Override
            protected void checkIfClusterAndResourceAllowed(String cluster, String resource) {
                if (!nodeIds.contains(resource)) throw new AuthorizationException(null);
            }
        };
    }

    @Test
    void nodePatternsHideVotersAndRestrictedLeaderIdentity() throws Exception {
        when(repository.get("test")).thenReturn(MetadataQuorum.builder()
            .status(MetadataQuorum.Status.AVAILABLE).leaderId(100).voters(List.of(
                MetadataQuorum.Voter.builder().id(100).leader(true).endpoints(List.of("CONTROLLER://private:9093")).build(),
                MetadataQuorum.Voter.builder().id(101).endpoints(List.of()).build())).build());
        var result = controller(true, Set.of("101")).quorum("test");
        assertEquals(List.of(101), result.getVoters().stream().map(MetadataQuorum.Voter::getId).toList());
        assertNull(result.getLeaderId());
    }

    @Test
    void deniedClusterDoesNotQueryKafka() {
        assertThrows(AuthorizationException.class, () -> controller(false, Set.of()).quorum("other"));
        verifyNoInteractions(repository);
    }
}
