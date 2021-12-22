package org.akhq.utils;

import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Getter;
import org.akhq.configs.newAcls.Binding;

import java.util.List;
import java.util.Map;

@Introspected
@Builder
@Getter
public class ClaimResponse {
    private List<Binding> bindings;
}
