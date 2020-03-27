package org.kafkahq.service.mapper;

import org.kafkahq.models.ConnectDefinition;
import org.kafkahq.service.dto.connect.ConnectDefinitionDTO;

import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ConnectMapper {

    public ConnectDefinitionDTO fromConnectDefinitionToConnectDefinitionDTO(ConnectDefinition connect) {
        return new ConnectDefinitionDTO(
                connect.getName(),
                connect.getConfigsAsJson(),
                connect.getType(),
                connect.isPaused(),
                connect.getShortClassName(),
                connect.getTasks()
                        .stream()
                        .map(this::fromTaskDefinitionToTaskDefinitionDTO)
                        .collect(Collectors.toList())
        );
    }

    private ConnectDefinitionDTO.TaskDefinitionDTO fromTaskDefinitionToTaskDefinitionDTO(ConnectDefinition.TaskDefinition taskDefinition) {
        return new ConnectDefinitionDTO.TaskDefinitionDTO(
                taskDefinition.getWorkerId(),
                taskDefinition.getId(),
                taskDefinition.getState()
        );
    }
}
