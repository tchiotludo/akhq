//package org.akhq.service.mapper;
//
//import org.akhq.models.AccessControl;
//import org.akhq.service.dto.acls.AclsDTO;
//
//import javax.inject.Singleton;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//@Singleton
//public class AclMapper {
//    public List<AclsDTO> fromAclListToAclDTOList(List<AccessControl> acls, String resourceType) {
//        List<AclsDTO> aclsList = new ArrayList<>();
//
//        acls.forEach(acl -> {
//            acl.getAcls().forEach(permission -> {
//                int index = Arrays.asList(keys.toArray()).indexOf(key);
//                List<String> value = values.get(index);
//
//                aclsList.add(new AclsDTO(acl.getPrincipal(), acl.getEncodedPrincipal(), permission.getResource(), permission.getHost(), permission.getResource()));
//            });
//        });
//
//        return aclsList;
//    }
//}
