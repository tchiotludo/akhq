package org.kafkahq.service.dto.topic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigOperationDTO {
    private String clusterId;
    private String topicId;
    private Map<String, String> configs;
}
