package org.akhq.models;

import io.micronaut.serde.annotation.Serdeable;
import lombok.*;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Serdeable
public class KsqlDbStream {
    private String name;
    private String topic;
    private String keyFormat;
    private String valueFormat;
    private Boolean isWindowed;
}
