package org.akhq.service;

import org.akhq.configs.Connect;
import org.akhq.modules.KafkaModule;
import org.akhq.service.dto.ConnectDTO;
import org.akhq.service.mapper.ConnectMapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class ConnectService {

    private KafkaModule kafkaModule;

    private ConnectMapper connectMapper;

    @Inject
    public ConnectService(KafkaModule kafkaModule, ConnectMapper connectMapper) {
        this.kafkaModule = kafkaModule;
        this.connectMapper = connectMapper;
    }

    public List<ConnectDTO> getAllConnectsFromCLuster(String clusterId) {
        return Optional.ofNullable(kafkaModule.getConnection(clusterId))
                .map(c -> convertToConnectDTO(c.getConnect()))
                .orElse(null);
    }

    private List<ConnectDTO> convertToConnectDTO(List<Connect> connects) {
        return connects
                .stream()
                .map(c -> connectMapper.fromConnectToConnectDTO(c))
                .collect(Collectors.toList());
    }
}
