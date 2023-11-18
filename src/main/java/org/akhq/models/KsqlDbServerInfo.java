package org.akhq.models;

import lombok.*;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KsqlDbServerInfo {
    private String serverVersion;
    private String kafkaClusterId;
    private String ksqlServiceId;
}
