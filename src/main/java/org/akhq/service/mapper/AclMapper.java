package org.akhq.service.mapper;

import org.akhq.models.AccessControlList;
import org.akhq.models.Cluster;
import org.akhq.service.dto.ClusterDTO;
import org.akhq.service.dto.acls.AclsDTO;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class AclMapper {
    public List<AclsDTO> fromAclListToAclDTOList(List<AccessControlList> acls, String resourceType) {
        List<AclsDTO> aclsList = new ArrayList<>();

        acls.forEach(acl -> {
            Map<AccessControlList.HostResource, List<String>> permissions =
                    acl
                            .getPermissions()
                            .get(resourceType.toLowerCase());
            Set<AccessControlList.HostResource> keys = permissions.keySet();
            List<List<String>> values = new ArrayList<>(permissions.values());

            keys.forEach(key -> {
                int index = Arrays.asList(keys.toArray()).indexOf(key);
                List<String> value = values.get(index);

                aclsList.add(new AclsDTO(acl.getPrincipal(), acl.getEncodedPrincipal(), key.getResource(), key.getHost(), value));
            });
        });

        return aclsList;
    }
}
