package org.akhq.service.dto.ConsumerGroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteConsumerGroupDTO {
    public String clusterId;
    public String groupId;
}
