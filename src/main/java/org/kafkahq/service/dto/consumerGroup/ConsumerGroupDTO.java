package org.kafkahq.service.dto.consumerGroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerGroupDTO {
    private String id;
    private String state;
    private int coordinator;
    private int members;
    private List<TopicLagDTO> topicLag;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicLagDTO{
        private String topicId;
        private Long lag;
    }
}
