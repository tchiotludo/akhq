package org.akhq.models;

import lombok.*;

import java.util.List;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TopicSchema {
    private List<Schema> key;
    private List<Schema> value;
}
