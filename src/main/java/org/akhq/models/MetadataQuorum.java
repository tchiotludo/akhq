package org.akhq.models;

import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** Metadata voters are kept separate from brokers: controller-only nodes have no broker log/config API. */
@Introspected
@Getter
@Builder(toBuilder = true)
public class MetadataQuorum {
    public enum Status { AVAILABLE, UNSUPPORTED, FORBIDDEN, TIMED_OUT, UNAVAILABLE }

    private Status status;
    private Integer leaderId;
    private Long leaderEpoch;
    private Long highWatermark;
    private long fetchedAt;
    private List<Voter> voters;

    @Introspected
    @Getter
    @Builder
    public static class Voter {
        private int id;
        private boolean leader;
        private List<String> endpoints;
        private Long logEndOffset;
        private Long lag;
        private Long lastFetchTimestamp;
        private Long lastCaughtUpTimestamp;
    }
}
