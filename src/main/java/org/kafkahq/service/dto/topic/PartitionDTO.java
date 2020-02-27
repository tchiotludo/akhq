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
    private List<ReplicaDTO> replicas;
    private OffsetsDTO offsets;
    private SizesDTO size;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReplicaDTO {
        private int id;
        private boolean inSync;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OffsetsDTO {
        private long firstOffset;
        private long lastOffset;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SizesDTO {
        private long minSize;
        private long maxSize;
    }
}
