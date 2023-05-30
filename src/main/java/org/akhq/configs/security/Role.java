package org.akhq.configs.security;

import lombok.Data;

import java.util.List;

@Data
public class Role {
    private List<Resource> resources;
    private List<Action> actions;

    public enum Resource {
        TOPIC,
        TOPIC_DATA,
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
        // CONSUMER_GROUP only
        UPDATE_OFFSET,
        DELETE_OFFSET,
        // TOPIC & NODE only
        READ_CONFIG,
        ALTER_CONFIG,
        // SCHEMA only
        DELETE_VERSION,
        // CONNECT only
        UPDATE_STATE,
        // KSQLDB only
        EXECUTE
    }
}