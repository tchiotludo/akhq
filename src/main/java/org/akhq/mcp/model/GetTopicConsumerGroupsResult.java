package org.akhq.mcp.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public record GetTopicConsumerGroupsResult(
    String cluster,
    String topic,
    int totalMatches,
    boolean truncated,
    List<Group> groups,
    String message
) {
    @Introspected
    public record Group(String group, String state, int memberCount, boolean activeOnTopic, long lagOnTopic) {
    }
}
