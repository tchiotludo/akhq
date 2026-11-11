package org.akhq.repositories;

import org.akhq.models.MetadataQuorum;
import org.akhq.modules.AbstractKafkaWrapper;
import org.apache.kafka.clients.admin.QuorumInfo;
import org.apache.kafka.clients.admin.RaftVoterEndpoint;
import org.apache.kafka.common.errors.ClusterAuthorizationException;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MetadataQuorumRepositoryTest {
    private final AbstractKafkaWrapper wrapper = mock(AbstractKafkaWrapper.class);
    private final MetadataQuorumRepository repository = new MetadataQuorumRepository(wrapper);

    private QuorumInfo.ReplicaState voter(int id, long offset) {
        var voter = mock(QuorumInfo.ReplicaState.class);
        when(voter.replicaId()).thenReturn(id);
        when(voter.logEndOffset()).thenReturn(offset);
        when(voter.lastFetchTimestamp()).thenReturn(OptionalLong.empty());
        when(voter.lastCaughtUpTimestamp()).thenReturn(OptionalLong.of(-1));
        return voter;
    }

    private QuorumInfo quorum() {
        var quorum = mock(QuorumInfo.class);
        var voters = List.of(voter(101, 90), voter(100, 100), voter(102, -1));
        var observers = List.of(voter(1, 100));
        when(quorum.leaderId()).thenReturn(100);
        when(quorum.leaderEpoch()).thenReturn(7L);
        when(quorum.highWatermark()).thenReturn(80L);
        when(quorum.voters()).thenReturn(voters);
        when(quorum.observers()).thenReturn(observers);
        when(quorum.nodes()).thenReturn(Map.of());
        return quorum;
    }

    @Test
    void staticQuorumShowsAllVotersWithoutEndpointsAndExcludesObservers() throws Exception {
        var quorum = quorum();
        when(wrapper.describeMetadataQuorum("test")).thenReturn(quorum);
        var result = repository.get("test");
        assertEquals(MetadataQuorum.Status.AVAILABLE, result.getStatus());
        assertEquals(List.of(100, 101, 102), result.getVoters().stream().map(MetadataQuorum.Voter::getId).toList());
        assertTrue(result.getVoters().getFirst().isLeader());
        assertEquals(10L, result.getVoters().get(1).getLag()); // leader LEO, not high watermark
        assertTrue(result.getVoters().stream().allMatch(v -> v.getEndpoints().isEmpty()));
        assertNull(result.getVoters().get(2).getLogEndOffset());
        assertNull(result.getVoters().get(2).getLag());
        assertNull(result.getVoters().getFirst().getLastCaughtUpTimestamp());
    }

    @Test
    void dynamicQuorumIncludesControllerEndpointsWithIpv6Brackets() throws Exception {
        var quorum = quorum();
        var node = mock(QuorumInfo.Node.class);
        when(node.endpoints()).thenReturn(List.of(new RaftVoterEndpoint("CONTROLLER", "::1", 9093)));
        when(quorum.nodes()).thenReturn(Map.of(100, node));
        when(wrapper.describeMetadataQuorum("test")).thenReturn(quorum);
        assertEquals(List.of("CONTROLLER://[::1]:9093"), repository.get("test").getVoters().getFirst().getEndpoints());
    }

    @Test
    void missingLeaderDoesNotInventLagOrLeaderId() throws Exception {
        var quorum = quorum();
        when(quorum.leaderId()).thenReturn(-1);
        when(wrapper.describeMetadataQuorum("test")).thenReturn(quorum);
        var result = repository.get("test");
        assertNull(result.getLeaderId());
        assertTrue(result.getVoters().stream().allMatch(v -> !v.isLeader() && v.getLag() == null));
    }

    @Test
    void leaderChangesAreNotCached() throws Exception {
        var first = quorum();
        var second = quorum();
        when(second.leaderId()).thenReturn(101);
        when(wrapper.describeMetadataQuorum("test")).thenReturn(first, second);
        assertEquals(100, repository.get("test").getLeaderId());
        assertEquals(101, repository.get("test").getLeaderId());
    }

    @Test
    void expectedFailuresHaveDistinctStatusAndNoFabricatedVoters() throws Exception {
        when(wrapper.describeMetadataQuorum("test")).thenThrow(
            new ExecutionException(new UnsupportedVersionException("unsupported")),
            new ExecutionException(new ClusterAuthorizationException("denied")),
            new java.util.concurrent.TimeoutException(),
            new ExecutionException(new org.apache.kafka.common.errors.TimeoutException("timeout")));
        for (var expected : List.of(MetadataQuorum.Status.UNSUPPORTED, MetadataQuorum.Status.FORBIDDEN,
            MetadataQuorum.Status.TIMED_OUT, MetadataQuorum.Status.TIMED_OUT)) {
            var result = repository.get("test");
            assertEquals(expected, result.getStatus());
            assertTrue(result.getVoters().isEmpty());
            assertNull(result.getLeaderId());
        }
    }

    @Test
    void unexpectedFailuresAndInterruptionsPropagate() throws Exception {
        when(wrapper.describeMetadataQuorum("test")).thenThrow(
            new ExecutionException(new IllegalStateException("unexpected")), new InterruptedException());
        assertThrows(ExecutionException.class, () -> repository.get("test"));
        assertThrows(InterruptedException.class, () -> repository.get("test"));
    }
}
