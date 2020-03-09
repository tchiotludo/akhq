package org.kafkahq.service.dto.ConsumerGroupd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kafkahq.models.Topic;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerGroupDTO {
    private String id;
    private String state;
    private int coordinator;
    private int members;
    private List<String> topics;
}
