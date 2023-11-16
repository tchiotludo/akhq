package org.akhq.models;

import io.micronaut.serde.annotation.Serdeable;
import lombok.*;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Serdeable
public class KsqlDbQuery {
    private String queryType;
    private String id;
    private String sql;
    private String sink;
    private String sinkTopic;
}
