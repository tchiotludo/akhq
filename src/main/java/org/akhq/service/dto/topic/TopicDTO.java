package org.akhq.service.dto.topic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import org.akhq.models.ConsumerGroup;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicDTO {

    private String name;
    private int count;
    private int size;
    private String total;
    private String factor;
    private String inSync;
    private List<ConsumerGroup> consumerGroups;

}
