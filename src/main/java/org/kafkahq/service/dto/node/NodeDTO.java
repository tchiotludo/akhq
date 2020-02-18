package org.kafkahq.service.dto.node;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeDTO {

    private int id;
    private String host;
    private int port;
    private String rack;
}
