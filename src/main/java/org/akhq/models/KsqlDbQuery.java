package org.akhq.models;

import lombok.*;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KsqlDbQuery {
    private String queryType;
    private String id;
    private String sql;
    private String sink;
    private String sinkTopic;
}
