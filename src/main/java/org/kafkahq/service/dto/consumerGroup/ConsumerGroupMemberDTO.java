package org.kafkahq.service.dto.consumerGroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerGroupMemberDTO {
    private String clientId;
    private String id;
    private String host;
    private  List<AssignmentDTO> assignments;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentDTO{
        private  String topic;
        private int partition;
    }
}
