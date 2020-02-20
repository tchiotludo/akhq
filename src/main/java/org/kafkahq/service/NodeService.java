package org.kafkahq.service;

import org.kafkahq.models.Config;
import org.kafkahq.models.LogDir;
import org.kafkahq.repositories.ConfigRepository;
import org.kafkahq.repositories.LogDirRepository;
import org.kafkahq.service.dto.node.ConfigDTO;
import org.kafkahq.service.dto.node.ConfigOperationDTO;
import org.kafkahq.service.dto.node.LogDTO;
import org.kafkahq.service.mapper.NodeMapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class NodeService {

    private NodeMapper nodeMapper;

    private ConfigRepository configRepository;

    private LogDirRepository logDirRepository;

    @Inject
    public NodeService(NodeMapper nodeMapper, ConfigRepository configRepository, LogDirRepository logDirRepository) {
        this.nodeMapper = nodeMapper;
        this.configRepository = configRepository;
        this.logDirRepository = logDirRepository;
    }

    public List<ConfigDTO> getConfigDTOList(String clusterId, Integer nodeId) throws ExecutionException, InterruptedException {
        List<Config> configList = this.configRepository.findByBroker(clusterId, nodeId);
        configList.sort((o1, o2) -> o1.isReadOnly() == o2.isReadOnly() ? 0 :
                (o1.isReadOnly() ? 1 : -1)
        );

        return configList.stream().map(config -> nodeMapper.fromConfigToConfigDTO(config)).collect(Collectors.toList());
    }

    public List<LogDTO> getLogDTOList(String clusterId, Integer nodeId) throws ExecutionException, InterruptedException {
        List<LogDir> logList = logDirRepository.findByBroker(clusterId, nodeId);

        return logList.stream().map(logDir -> nodeMapper.fromLogDirToLogDTO(logDir)).collect(Collectors.toList());
    }

    public List<ConfigDTO> updateConfigs(ConfigOperationDTO configOperation) throws ExecutionException, InterruptedException {
        return ConfigRepository.updatedConfigs(configOperation.getConfigs(), this.configRepository.findByBroker(configOperation.getClusterId(), configOperation.getNodeId()))
                .stream()
                .map(config -> nodeMapper.fromConfigToConfigDTO(config))
                .collect(Collectors.toList());
    }
}
