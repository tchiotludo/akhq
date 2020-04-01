package org.akhq.service.mapper;

import org.akhq.models.ConnectDefinition;
import org.akhq.service.dto.connect.ConnectDefinitionDTO;

import javax.inject.Singleton;
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
