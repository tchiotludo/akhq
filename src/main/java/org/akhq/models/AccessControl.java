package org.akhq.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;

import java.util.*;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class AccessControl {
    private String principal;
    private List<Acl> acls;

    @JsonIgnore
    private String encodedPrincipal;

    public static String encodePrincipal(String principal) {
        return Base64.getEncoder().encodeToString(principal.getBytes());
    }

    public static String decodePrincipal(String encodedPrincipal) {
        return new String(Base64.getDecoder().decode(encodedPrincipal));
    }

    public AccessControl(String principal, Collection<AclBinding> aclBinding) {
        this.principal = principal;
        this.encodedPrincipal = encodePrincipal(this.principal);
        this.acls = aclBinding
            .stream()
            .map(Acl::new)
            .collect(Collectors.toList());
    }

    public List<Acl> findByRessourceType(String resourceType) {
        return findByRessourceType(ResourceType.valueOf(resourceType.toUpperCase()));
    }

    public List<Acl> findByRessourceType(ResourceType resourceType) {
        return this.acls
            .stream()
            .filter(acl -> acl.getResource().getResourceType() == resourceType)
            .collect(Collectors.toList());
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @NoArgsConstructor
    public static class Acl {
        private String host;
        private Operation operation;
        private Resource resource;

        public Acl(AclBinding aclBinding) {
            this.host = aclBinding.entry().host();
            this.operation = new Operation(aclBinding.entry());
            this.resource = new Resource(aclBinding.pattern());
        }
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @NoArgsConstructor
    public static class Operation {
        private AclOperation operation;
        private AclPermissionType permissionType;

        public Operation(AccessControlEntry aclOperation) {
            this.operation = aclOperation.operation();
            this.permissionType = aclOperation.permissionType();
        }
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @NoArgsConstructor
    public static class Resource {
        private ResourceType resourceType;
        private String name;
        private PatternType patternType;

        public Resource(ResourcePattern resourcePattern) {
            this.resourceType = resourcePattern.resourceType();
            this.name = resourcePattern.name();
            this.patternType = resourcePattern.patternType();
        }
    }
}
