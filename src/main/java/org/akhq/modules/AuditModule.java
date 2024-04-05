package org.akhq.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.security.authentication.Authentication;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.Audit;
import org.akhq.models.audit.AuditEvent;
import io.micronaut.security.utils.SecurityService;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.time.LocalDateTime;
import java.util.Optional;

@Singleton
@Slf4j
public class AuditModule {
    SecurityService securityService;

    @Inject
    KafkaModule kafkaModule;

    @Inject
    Audit auditConfig;

    @Value("${micronaut.security.enabled}")
    Boolean securityEnabled;

    @Inject
    ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        if (securityEnabled) {
            securityService = applicationContext.getBean(SecurityService.class);
        } else {
            securityService = new NoOpSecurityService();
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();

    public void save(AuditEvent event) {

        final String clusterId = auditConfig.getClusterId();
        final String topicName = auditConfig.getTopicName();

        if (!auditConfig.getEnabled()) {
            return;
        }


        byte[] value;
        securityService.username().ifPresent(event::setUserName);
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

    private static class NoOpSecurityService implements SecurityService {

        @Override
        public Optional<String> username() {
            return Optional.empty();
        }

        @Override
        public Optional<Authentication> getAuthentication() {
            return Optional.empty();
        }

        @Override
        public boolean isAuthenticated() {
            return false;
        }

        @Override
        public boolean hasRole(String role) {
            return false;
        }
    }

}
