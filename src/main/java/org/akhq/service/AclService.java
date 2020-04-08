package org.akhq.service;

import org.akhq.models.AccessControlList;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.repositories.AbstractRepository;
import org.akhq.repositories.AccessControlListRepository;
import org.akhq.service.dto.acls.AclsDTO;
import org.akhq.service.mapper.AclMapper;
import org.apache.kafka.common.resource.ResourceType;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class AclService extends AbstractRepository {
    private AbstractKafkaWrapper kafkaWrapper;
    private AccessControlListRepository aclRepository;
    private AclMapper aclMapper;

    public AclService(AbstractKafkaWrapper kafkaWrapper, AccessControlListRepository aclRepository, AclMapper aclMapper) {
        this.kafkaWrapper = kafkaWrapper;
        this.aclRepository = aclRepository;
        this.aclMapper = aclMapper;
    }

    public List<AclsDTO> findAll(String clusterId, Optional<String> search) {
        return aclRepository.findAll(clusterId, search)
                .stream()
                .map(
                        acl -> new AclsDTO(
                                acl.getPrincipal(),
                                acl.getEncodedPrincipal(),
                                "",
                                "",
                                new ArrayList<>())).collect(Collectors.toList()
                );
    }

    public List<AclsDTO> findByPrincipal(String clusterId, String encodedPrincipal, String resourceType) {
        AccessControlList acl = aclRepository.findByPrincipal(clusterId, encodedPrincipal, Optional.of(resourceType));

        return aclMapper.fromAclListToAclDTOList(Collections.singletonList(acl), resourceType);
    }

    public List<AclsDTO> getAcls(String clusterId, ResourceType resourceType, String id) {
        return aclMapper.fromAclListToAclDTOList(aclRepository.findByResourceType(clusterId, resourceType, id), resourceType.toString());
    }
}
