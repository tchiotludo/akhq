package org.akhq.configs.newAcls;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Permission {
    private Resource resource;
    private List<Role> roles;

    public enum Resource {
        TOPIC,
        CONSUMER_GROUP,
        CONNECT_CLUSTER,
        CONNECTOR,
        SCHEMA,
        NODE,
        ACLS
    }

    public enum Role {
        READ,
        CREATE,
        UPDATE,
        DELETE,
        // Topic specific
        PRODUCE,
        CONSUME,
        // Topic & Node specific
        ALTER_CONFIG,
        READ_CONFIG,
        // Connect specific
        UPDATE_STATE
    }

}
