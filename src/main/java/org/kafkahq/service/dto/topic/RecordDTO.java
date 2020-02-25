package org.kafkahq.service.dto.topic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordDTO {
    private String key;
    private String value;
    private LocalDateTime date;
    private int partition;
    private long offset;
    private Map<String, String> headers;
    private Pair<Integer, Integer> schema;
}
