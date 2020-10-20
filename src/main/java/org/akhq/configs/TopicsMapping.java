package org.akhq.configs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopicsMapping {
    String topicRegex;
    String descriptorFileBase64;
    String keyMessageType;
    String valueMessageType;
}
