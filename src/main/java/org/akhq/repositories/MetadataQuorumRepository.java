package org.akhq.repositories;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.akhq.models.MetadataQuorum;
import org.akhq.modules.AbstractKafkaWrapper;
import org.apache.kafka.clients.admin.QuorumInfo;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.UnsupportedVersionException;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ExecutionException;

@Singleton
public class MetadataQuorumRepository {
    private final AbstractKafkaWrapper kafkaWrapper;

    @Inject
    public MetadataQuorumRepository(AbstractKafkaWrapper kafkaWrapper) {
        this.kafkaWrapper = kafkaWrapper;
    }

    public MetadataQuorum get(String clusterId) throws InterruptedException, ExecutionException {
        try {
            return from(kafkaWrapper.describeMetadataQuorum(clusterId));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof UnsupportedVersionException) {
                return unavailable(MetadataQuorum.Status.UNSUPPORTED);
            } else if (cause instanceof AuthorizationException) {
                return unavailable(MetadataQuorum.Status.FORBIDDEN);
            } else if (cause instanceof TimeoutException) {
                return unavailable(MetadataQuorum.Status.TIMED_OUT);
            }
            throw e;
        } catch (UnsupportedVersionException e) {
            return unavailable(MetadataQuorum.Status.UNSUPPORTED);
        } catch (AuthorizationException e) {
            return unavailable(MetadataQuorum.Status.FORBIDDEN);
        } catch (TimeoutException | java.util.concurrent.TimeoutException e) {
            return unavailable(MetadataQuorum.Status.TIMED_OUT);
        }
    }

    static MetadataQuorum from(QuorumInfo quorum) {
        OptionalLong leaderOffset = quorum.voters().stream()
            .filter(voter -> voter.replicaId() == quorum.leaderId())
            .mapToLong(QuorumInfo.ReplicaState::logEndOffset)
            .filter(offset -> offset >= 0)
            .findFirst();

        List<MetadataQuorum.Voter> voters = quorum.voters().stream()
            .sorted(Comparator.comparingInt(QuorumInfo.ReplicaState::replicaId))
            .map(voter -> {
                QuorumInfo.Node node = quorum.nodes().get(voter.replicaId());
                // Static KRaft quorums may report voter IDs without endpoints (KIP-853 not enabled).
                List<String> endpoints = node == null ? List.of() : node.endpoints().stream()
                    .map(endpoint -> endpoint.name() + "://" +
                        (endpoint.host().contains(":") ? "[" + endpoint.host() + "]" : endpoint.host()) +
                        ":" + endpoint.port())
                    .toList();
                Long offset = voter.logEndOffset() < 0 ? null : voter.logEndOffset();
                Long lag = offset != null && leaderOffset.isPresent() && leaderOffset.getAsLong() >= offset
                    ? leaderOffset.getAsLong() - offset : null;
                return MetadataQuorum.Voter.builder()
                    .id(voter.replicaId())
                    .leader(voter.replicaId() == quorum.leaderId())
                    .endpoints(endpoints)
                    .logEndOffset(offset)
                    .lag(lag)
                    .lastFetchTimestamp(timestamp(voter.lastFetchTimestamp()))
                    .lastCaughtUpTimestamp(timestamp(voter.lastCaughtUpTimestamp()))
                    .build();
            }).toList();

        return MetadataQuorum.builder()
            .status(MetadataQuorum.Status.AVAILABLE)
            .leaderId(quorum.leaderId() < 0 ? null : quorum.leaderId())
            .leaderEpoch(quorum.leaderEpoch() < 0 ? null : quorum.leaderEpoch())
            .highWatermark(quorum.highWatermark() < 0 ? null : quorum.highWatermark())
            .fetchedAt(System.currentTimeMillis())
            .voters(voters)
            .build();
    }

    private static Long timestamp(OptionalLong timestamp) {
        return timestamp.isPresent() && timestamp.getAsLong() >= 0 ? timestamp.getAsLong() : null;
    }

    private static MetadataQuorum unavailable(MetadataQuorum.Status status) {
        return MetadataQuorum.builder().status(status).fetchedAt(System.currentTimeMillis()).voters(List.of()).build();
    }
}
