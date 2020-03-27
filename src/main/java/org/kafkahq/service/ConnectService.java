package org.kafkahq.service;

import org.kafkahq.modules.KafkaModule;
import org.kafkahq.repositories.ConnectRepository;
import org.kafkahq.service.dto.connect.ConnectDefinitionDTO;
import org.kafkahq.service.dto.connect.DeleteConnectDefinitionDTO;
import org.kafkahq.service.mapper.ConnectMapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
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
        return this.connectMapper.fromConnectDefinitionToConnectDefinitionDTO(this.connectRepository.getDefinition(clusterId, connectId, definitionId));
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
}
