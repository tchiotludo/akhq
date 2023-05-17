package org.akhq.models.security;

import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Getter;
import org.akhq.configs.security.Group;

import java.util.List;
import java.util.Map;

@Introspected
@Builder
@Getter
public class ClaimResponse {
    private Map<String, List<Group>> groups;
}
