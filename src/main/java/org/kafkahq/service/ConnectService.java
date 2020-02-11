package org.kafkahq.service;

import org.kafkahq.configs.Connect;
import org.kafkahq.models.Cluster;
import org.kafkahq.modules.KafkaModule;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class ConnectService {

    private KafkaModule kafkaModule;

    @Inject
    public ConnectService(KafkaModule kafkaModule) {
        this.kafkaModule = kafkaModule;
    }

    public List<Connect> getAllConnectsFromCLuster(String clusterId) {
        return Optional.ofNullable(kafkaModule.getConnection(clusterId))
                .map(c -> c.getConnect())
                .orElse(null);
    }
}
