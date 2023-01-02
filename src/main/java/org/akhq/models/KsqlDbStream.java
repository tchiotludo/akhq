package org.akhq.models;

import lombok.*;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KsqlDbStream {
    private String name;
    private String topic;
    private String keyFormat;
    private String valueFormat;
    private Boolean isWindowed;
}
