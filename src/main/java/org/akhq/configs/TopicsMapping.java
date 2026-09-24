package org.akhq.configs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopicsMapping {
    String topicRegex;
    String descriptorFile;
    String descriptorFileBase64;
    String keyMessageType;
    String valueMessageType;

    // BSR specific fields
    String bsrCommit;         // Specific commit/version (optional, can use headers instead)
    String bsrMessageType;    // Full protobuf message name for BSR (e.g., "com.myorg.Order")
}
