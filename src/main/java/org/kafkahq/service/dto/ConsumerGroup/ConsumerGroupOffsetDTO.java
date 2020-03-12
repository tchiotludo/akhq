package org.kafkahq.service.dto.ConsumerGroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kafkahq.models.Consumer;
import org.kafkahq.models.TopicPartition;
import org.kafkahq.service.dto.topic.TopicDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsumerGroupOffsetDTO {
    private String name;
    private int partition;
    private Optional<Consumer> member;
    private Optional<Long> offset;
    private Optional<Long> lag;


}
