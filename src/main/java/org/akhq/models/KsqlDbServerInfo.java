package org.akhq.models;

import io.micronaut.serde.annotation.Serdeable;
import lombok.*;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Serdeable
public class KsqlDbServerInfo {
    private String serverVersion;
    private String kafkaClusterId;
    private String ksqlServiceId;
}
