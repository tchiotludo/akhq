package org.akhq.service.dto.node;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogDTO {
    private Integer broker;
    private String topic;
    private int partition;
    private long size;
    private long offsetLag;
}
