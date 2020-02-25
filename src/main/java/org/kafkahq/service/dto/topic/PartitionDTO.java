package org.kafkahq.service.dto.topic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartitionDTO {
    private int id;
    private int leader;
    private List<Pair<Integer, Boolean>> replicas;
    private Pair<Long, Long> offsets;
    private Pair<Long, Long> size;
}
