package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.bind.annotation.Bindable;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@ConfigurationProperties("akhq.audit")
public class Audit {
    @NotBlank
    Boolean enabled = false;
    String clusterId;
    String topicName;
}
