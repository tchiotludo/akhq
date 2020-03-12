package org.kafkahq.service.dto.ConsumerGroupd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kafkahq.service.dto.topic.TopicDTO;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerGroupListDTO {
    private List<ConsumerGroupDTO> ConsumerGroups;
    private int totalPageNumber;
}

