package org.akhq.service;

import org.akhq.models.AccessControlList;
import org.akhq.repositories.AccessControlListRepository;
import org.akhq.service.dto.acls.AclsDTO;
import org.apache.kafka.common.resource.ResourceType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class AclsService {

    private AccessControlListRepository aclRepository;

    @Inject
    public AclsService(AccessControlListRepository aclRepository) {
        this.aclRepository = aclRepository;
    }

    public List<List<AclsDTO>> getAcls(String clusterId, ResourceType resourceType, String id) {
        return aclRepository.findByResourceType(clusterId, resourceType, id).stream().map(acl -> {
            Map<AccessControlList.HostResource, List<String>> permissions =
                    acl
                            .getPermissions()
                            .get(resourceType.toString().toLowerCase());
            Set<AccessControlList.HostResource> keys = permissions.keySet();
            List<List<String>> values = new ArrayList<>(permissions.values());

            return keys.stream().map(key -> {
                int index = Arrays.asList(keys.toArray()).indexOf(key);
                List<String> value = values.get(index);

                return new AclsDTO(acl.getPrincipal(), key.getHost(), value);
            }).collect(Collectors.toList());
        }).collect(Collectors.toList());
    }
}
