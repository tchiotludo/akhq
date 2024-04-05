package org.akhq.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.security.utils.SecurityService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.Audit;
import org.akhq.models.audit.AuditEvent;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Optional;

@Singleton
@Slf4j
public class AuditModule {
    @Inject
    Optional<SecurityService> securityService;

    @Inject
    KafkaModule kafkaModule;

    @Inject
    Audit auditConfig;

    private final ObjectMapper mapper = new ObjectMapper();

    public void save(AuditEvent event) {

        final String clusterId = auditConfig.getClusterId();
        final String topicName = auditConfig.getTopicName();

        if (!auditConfig.getEnabled()) {
            return;
        }


        byte[] value;
        securityService.flatMap(SecurityService::username).ifPresent(event::setUserName);
        try {
            value = mapper.writeValueAsBytes(event);
        } catch (Exception e) {
            log.error("Audit event cannot be serialized to JSON", e);
            return;
        }

        kafkaModule.getProducer(clusterId).send(new ProducerRecord<>(topicName, value), (metadata, exception) -> {
            if (exception != null) {
                log.error("Audit data cannot be sent to Kafka", exception);
            }
        });
    }

}
