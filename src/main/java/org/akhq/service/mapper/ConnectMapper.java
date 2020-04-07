package org.akhq.service.mapper;

import org.akhq.models.ConnectDefinition;
import org.akhq.models.ConnectPlugin;
import org.akhq.service.dto.connect.ConnectDefinitionConfigsDTO;
import org.akhq.service.dto.connect.ConnectDefinitionDTO;
import org.akhq.service.dto.connect.ConnectPluginDTO;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ConnectMapper {

    public ConnectDefinitionDTO fromConnectDefinitionToConnectDefinitionDTO(ConnectDefinition definition) {
        return new ConnectDefinitionDTO(
                definition.getName(),
                definition.getConfigsAsJson(),
                definition.getType(),
                definition.isPaused(),
                definition.getShortClassName(),
                definition.getTasks()
                        .stream()
                        .map(this::fromTaskDefinitionToTaskDefinitionDTO)
                        .collect(Collectors.toList())
        );
    }

    public ConnectDefinitionConfigsDTO fromConnectDefinitionToConnectDefinitionConfigsDTO(
            ConnectDefinition definition, ConnectPlugin pluginDefinition) {

        return new ConnectDefinitionConfigsDTO(
                fromConnectPluginToConnectPluginDTO(pluginDefinition),
                definition.getConfigs()
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

        return new ConnectPluginDTO(plugin.getClassName(), plugin.getShortClassName(), plugin.getType(), plugin.getVersion(), definitionsDTO);
    }
}
