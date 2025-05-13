package org.akhq.models.security;

import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Introspected
@Builder
@Getter
@Setter
@EqualsAndHashCode
public class ClaimRequest {
    ClaimProviderType providerType;
    String providerName;
    String username;
    List<String> groups;
}
