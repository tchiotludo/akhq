package org.akhq.service;

import org.akhq.models.ConnectDefinition;
import org.akhq.models.ConnectPlugin;
import org.akhq.modules.KafkaModule;
import org.akhq.repositories.ConnectRepository;
import org.akhq.service.dto.connect.ConnectDefinitionConfigsDTO;
import org.akhq.service.dto.connect.ConnectDefinitionDTO;
import org.akhq.service.dto.connect.ConnectPluginDTO;
import org.akhq.service.dto.connect.CreateConnectDefinitionDTO;
import org.akhq.service.dto.connect.DeleteConnectDefinitionDTO;
import org.akhq.service.mapper.ConnectMapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Singleton
public class ConnectService {

    private KafkaModule kafkaModule;

    private ConnectMapper connectMapper;

    private ConnectRepository connectRepository;

    @Inject
    public ConnectService(KafkaModule kafkaModule, ConnectMapper connectMapper, ConnectRepository connectRepository) {
        this.kafkaModule = kafkaModule;
        this.connectMapper = connectMapper;
        this.connectRepository = connectRepository;
    }

    public List<String> getAllConnectNames(String clusterId) {
        return new ArrayList<>(this.kafkaModule.getConnectRestClient(clusterId).keySet());
    }

    public List<ConnectDefinitionDTO> getConnectDefinitions(String clusterId, String connectId) {
        return this.connectRepository.getDefinitions(clusterId, connectId)
                .stream()
                .map(connectDefinition -> connectMapper.fromConnectDefinitionToConnectDefinitionDTO(connectDefinition))
                .collect(Collectors.toList());
    }

    public ConnectDefinitionDTO getConnectDefinition(String clusterId, String connectId, String definitionId) {
        return this.connectMapper.fromConnectDefinitionToConnectDefinitionDTO(
                this.connectRepository.getDefinition(clusterId, connectId, definitionId)
        );
    }

    public ConnectDefinitionConfigsDTO getConnectDefinitionConfigs(String clusterId, String connectId, String definitionId) {
        ConnectDefinition connectDefinition =
                this.connectRepository.getDefinition(clusterId, connectId, definitionId);

        String className = connectDefinition.getShortClassName();

        ConnectPlugin pluginDefinition = this.connectRepository.getPlugin(
                clusterId, connectId, className
        ).orElse(null);

        return this.connectMapper.fromConnectDefinitionToConnectDefinitionConfigsDTO(connectDefinition, pluginDefinition);
    }

    public void pauseConnectDefinition(String clusterId, String connectId, String definitionId) {
        this.connectRepository.pause(clusterId, connectId, definitionId);
    }

    public void restartConnectDefinition(String clusterId, String connectId, String definitionId) {
        this.connectRepository.restart(clusterId, connectId, definitionId);
    }

    public void restartTask(String clusterId, String connectId, String definitionId, int taskId) {
        this.connectRepository.restartTask(clusterId, connectId, definitionId, taskId);
    }

    public void resumeConnectDefinition(String clusterId, String connectId, String definitionId) {
        this.connectRepository.resume(clusterId, connectId, definitionId);
    }

    public void addConnectDefinition(CreateConnectDefinitionDTO createConnectDefinitionDTO) {
        Map<String, String> validConfigs =
                ConnectRepository.validConfigs(
                        createConnectDefinitionDTO.getConfigs(),
                        createConnectDefinitionDTO.getTransformsValue()
                );

        this.connectRepository.create(
                createConnectDefinitionDTO.getClusterId(),
                createConnectDefinitionDTO.getConnectId(),
                createConnectDefinitionDTO.getName(),
                validConfigs
        );
    }

    public List<ConnectDefinitionDTO> deleteConnectDefinition(DeleteConnectDefinitionDTO deleteConnectDefinitionDTO) {
        this.connectRepository.delete(
                deleteConnectDefinitionDTO.getClusterId(),
                deleteConnectDefinitionDTO.getConnectId(),
                deleteConnectDefinitionDTO.getDefinitionId()
        );

        return this.connectRepository.getDefinitions(
                deleteConnectDefinitionDTO.getClusterId(),
                deleteConnectDefinitionDTO.getConnectId()
        )
                .stream()
                .map(connectDefinition -> connectMapper.fromConnectDefinitionToConnectDefinitionDTO(connectDefinition))
                .collect(Collectors.toList());
    }

    public List<ConnectPluginDTO> getConnectPlugins(String clusterId, String connectId) {
        List<ConnectPlugin> plugins = connectRepository.getPlugins(clusterId, connectId);
        List<ConnectPluginDTO> pluginsDTO = new ArrayList<>();
        for (int i = 0; i < plugins.size(); i++) {
            ConnectPluginDTO plugin = connectMapper.fromConnectPluginToConnectPluginDTO(plugins.get(i));
            pluginsDTO.add(plugin);
        }
        return pluginsDTO;
    }
/*
   public ConnectPluginDTO getConnectPlugin (String clusterId, String connectId, String className) {
           return connectMapper.fromConnectPluginToConnectPluginDTO(connectRepository.getPlugin(clusterId, connectId, className));

   }

 */
}
