package org.akhq.configs.security;

import lombok.Data;

import java.util.List;

@Data
public class Role {
    private List<Resource> resources;
    private List<Action> actions;

    public enum Resource {
        TOPIC,
        CONSUMER_GROUP,
        CONNECT_CLUSTER,
        CONNECTOR,
        SCHEMA,
        NODE,
        ACL,
        KSQLDB
    }

    public enum Action {
        READ,
        CREATE,
        UPDATE,
        DELETE,
        // TOPIC only
        CONSUME,
        PRODUCE,
        // CONSUMER_GROUP only
        UPDATE_OFFSET,
        DELETE_OFFSET,
        // TOPIC & NODE only
        ALTER_CONFIG,
        READ_CONFIG,
        // SCHEMA only
        DELETE_VERSION,
        // CONNECT only
        UPDATE_STATE,
        // KSQLDB only
        EXECUTE
    }
}