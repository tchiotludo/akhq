package org.akhq.service.dto.consumerGroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupedTopicOffsetDTO {
    private Map<String, List<OffsetDTO>> groupedTopicOffset;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OffsetDTO {
        private int partition;
        private long offset;
        private long firstOffset;
        private long lastOffset;
    }
}
