package org.akhq.service;

import org.akhq.models.AccessControlList;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.repositories.AbstractRepository;
import org.apache.kafka.common.acl.AclBindingFilter;

import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class AclService extends AbstractRepository {
    private AbstractKafkaWrapper kafkaWrapper;

    public AclService(AbstractKafkaWrapper kafkaWrapper) {
        this.kafkaWrapper = kafkaWrapper;
    }

    public List<String> findAll(String clusterId, Optional<String> search) {
        try {
            return kafkaWrapper.describeAcls(clusterId, AclBindingFilter.ANY).stream()
                    .map(acl -> acl.entry().principal())
                    .collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException ex) {
            throw new CompletionException(ex);
        }
    }
}
