package org.akhq.utils;

import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Introspected
@Builder
@Getter
public class ClaimResponse {
    private List<String> roles;
    private List<String> topicsFilterRegexp;
    private List<String> connectsFilterRegexp;
    private List<String> consumerGroupsFilterRegexp;

    public Map<String, Object> getAttributes() {
        return Map.of(
            "topicsFilterRegexp", topicsFilterRegexp,
            "connectsFilterRegexp", connectsFilterRegexp,
            "consumerGroupsFilterRegexp", consumerGroupsFilterRegexp
        );
    }
}
