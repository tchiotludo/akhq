package org.akhq.service.dto.topic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigDTO {
    private String name;
    private String value;
    private String description;
    private Source type;
    private DataType dataType;
    private boolean isReadOnly;
    private boolean isSensitive;

    public enum Source {
        DYNAMIC_TOPIC_CONFIG,
        DYNAMIC_BROKER_CONFIG,
        DYNAMIC_DEFAULT_BROKER_CONFIG,
        STATIC_BROKER_CONFIG,
        DEFAULT_CONFIG,
        UNKNOWN
    }

    public enum DataType {
        TEXT, MILLI, BYTES;
    }
}
