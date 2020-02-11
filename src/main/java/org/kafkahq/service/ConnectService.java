package org.kafkahq.service;

import org.kafkahq.configs.Connect;
import org.kafkahq.models.Cluster;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.service.dto.ConnectDTO;
import org.kafkahq.service.mapper.ConnectMapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.text.html.Option;
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
