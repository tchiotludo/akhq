package org.kafkahq.service.dto.ConsumerGroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerGroupListDTO {
    private List<ConsumerGroupDTO> ConsumerGroups;
    private int totalPageNumber;
}

