package org.kafkahq.service.dto.topic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogDTO {
        private int broker;
        private String topic;
        private int partition;
        private long size;
        private long offsetLag;
}
