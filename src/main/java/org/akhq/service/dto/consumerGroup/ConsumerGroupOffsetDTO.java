package org.akhq.service.dto.consumerGroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.akhq.models.Consumer;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsumerGroupOffsetDTO {
    private String name;
    private int partition;
    private Consumer member;
    private Long offset;
    private Long lag;


}
