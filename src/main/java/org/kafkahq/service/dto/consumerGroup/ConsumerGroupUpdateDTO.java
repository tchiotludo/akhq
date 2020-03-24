package org.kafkahq.service.dto.consumerGroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsumerGroupUpdateDTO {
    private String clusterId;
    private String groupId;
    private Map<String, Long> offsets;
}
