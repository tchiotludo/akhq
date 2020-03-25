package org.akhq.service.dto.topic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProduceTopicDTO {
    @NotNull
    private String clusterId;
    @NotNull
    private String topicId;
    @NotNull
    private String value;
    @NotNull
    private Map<String, String> headers;
    @NotNull
    private String key;
    @Nullable
    private Integer partition;
    @Nullable
    private String timestamp;

}
