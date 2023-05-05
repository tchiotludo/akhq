package org.akhq.models.security;

import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Getter;
import org.akhq.configs.security.Group;

import java.util.List;

@Introspected
@Builder
@Getter
public class ClaimResponse {
    private List<Group> groups;
}
