package org.akhq.models;

import lombok.*;

import java.util.List;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KsqlDbQueryResult {
    private List<String> columnNames;
    private List<String> columnValues;
}
