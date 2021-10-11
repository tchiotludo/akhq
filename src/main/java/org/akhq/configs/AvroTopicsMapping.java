package org.akhq.configs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvroTopicsMapping {
    String topicRegex;
    String keySchemaFile;
    String valueSchemaFile;
}
