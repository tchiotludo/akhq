package org.akhq.service;

import org.akhq.models.AccessControlList;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.repositories.AbstractRepository;
import org.akhq.repositories.AccessControlListRepository;
import org.akhq.service.dto.acls.AclsDTO;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.resource.ResourceType;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class AclService extends AbstractRepository {
    private AbstractKafkaWrapper kafkaWrapper;
    private AccessControlListRepository aclRepository;

    public AclService(AbstractKafkaWrapper kafkaWrapper, AccessControlListRepository aclRepository) {
        this.kafkaWrapper = kafkaWrapper;
        this.aclRepository = aclRepository;
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

    public List<AclsDTO> getAcls(String clusterId, ResourceType resourceType, String id) {
        List<AclsDTO> aclsList = new ArrayList<>();

        aclRepository.findByResourceType(clusterId, resourceType, id).forEach(acl -> {
            Map<AccessControlList.HostResource, List<String>> permissions =
                    acl
                            .getPermissions()
                            .get(resourceType.toString().toLowerCase());
            Set<AccessControlList.HostResource> keys = permissions.keySet();
            List<List<String>> values = new ArrayList<>(permissions.values());

            keys.forEach(key -> {
                int index = Arrays.asList(keys.toArray()).indexOf(key);
                List<String> value = values.get(index);

                aclsList.add(new AclsDTO(acl.getPrincipal(), key.getHost(), value));
            });
        });

        return aclsList;
    }
}
