package org.akhq.service.mapper;

import org.akhq.models.ConnectDefinition;
import org.akhq.models.ConnectPlugin;
import org.akhq.service.dto.connect.ConnectDefinitionDTO;
import org.akhq.service.dto.connect.ConnectPluginDTO;

import javax.inject.Singleton;
import java.util.ArrayList;
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

    public ConnectPluginDTO fromConnectPluginToConnectPluginDTO(ConnectPlugin plugin) {
        List<ConnectPlugin.Definition> definitions = plugin.getDefinitions();
        List<ConnectPluginDTO.DefinitionDTO> definitionsDTO = new ArrayList<>();
        for (ConnectPlugin.Definition definition : definitions) {
            if (!definition.getName().equals("name") && !definition.getName().equals("connector.class")) {
                definitionsDTO.add(
                        new ConnectPluginDTO.DefinitionDTO(
                                definition.getName(),
                                definition.getType(),
                                definition.isRequired(),
                                definition.getDefaultValue(),
                                definition.getImportance(),
                                definition.getDocumentation(),
                                definition.getGroup(),
                                definition.getWidth(),
                                definition.getDisplayName(),
                                definition.getDependents(),
                                definition.getOrder()));
            }
        }
        return new ConnectPluginDTO(plugin.getClassName(), plugin.getType(), plugin.getVersion(), definitionsDTO);
    }
}
