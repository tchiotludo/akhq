package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties("akhq.audit")
public class Audit {
    Boolean enabled = false;
    String clusterId;
    String topicName;
}
