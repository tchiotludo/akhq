package org.akhq.configs.newAcls;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(Permission.Resource resourceType, Permission.Role role, String cluster, String resourceName){
        super(String.format("Not allowed to {} {} {} on cluster {}", role, resourceType, resourceName, cluster));
    }
}
