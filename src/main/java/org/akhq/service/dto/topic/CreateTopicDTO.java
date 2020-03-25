package org.akhq.service.dto.topic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTopicDTO {
    private String clusterId;
    private String topicId;
    private int partition;
    private short replicatorFactor;
    private String cleanupPolicy;
    private String retention;
}
