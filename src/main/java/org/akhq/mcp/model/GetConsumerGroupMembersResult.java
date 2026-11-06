package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record GetConsumerGroupMembersResult(
    String cluster,
    String group,
    int totalMembers,
    boolean truncated,
    List<Member> members,
    String message
) {
    @Introspected
    public record Member(String id, String clientId, String host, List<Assignment> assignments) {
    }

    @Introspected
    public record Assignment(String topic, List<Integer> partitions) {
    }
}
