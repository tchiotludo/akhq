package org.akhq.utils;

import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Introspected
@Builder
@Getter
@Setter
public class ClaimRequest {
    ClaimProviderType providerType;
    String providerName;
    String username;
    List<String> groups;
}
