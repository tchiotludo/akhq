package org.kafkahq.service.dto.topic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProduceTopicDTO {
    private String clusterId;
    private String topicId;
    private Optional<Integer> partition;
    private String value;
    private Optional<String> key;
    private Optional<String> timestamp;
   private  Map<String, List<String>> headers;
}


