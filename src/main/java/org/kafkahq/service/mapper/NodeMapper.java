package org.kafkahq.service.mapper;

import org.kafkahq.models.Node;
import org.kafkahq.service.dto.NodeDTO;

import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class NodeMapper {

    public NodeDTO fromNodeToNodeDTO(Node node) {
        return new NodeDTO(node.getId(), node.getHost(), node.getPort(), node.getRack());
    }
}
