package org.akhq.models;

import io.micronaut.serde.annotation.Serdeable;
import lombok.*;

import java.util.List;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Serdeable
public class KsqlDbQueryResult {
    private List<String> columnNames;
    private List<String> columnValues;
}
