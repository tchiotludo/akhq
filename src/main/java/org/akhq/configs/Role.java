package org.akhq.configs;

public class Role {
    public static final String ROLE_TOPIC_READ = "topic/read";
    public static final String ROLE_TOPIC_INSERT = "topic/insert";
    public static final String ROLE_TOPIC_DELETE = "topic/delete";
    public static final String ROLE_TOPIC_CONFIG_UPDATE = "topic/config/update";

    public static final String ROLE_NODE_READ = "node/read";
    public static final String ROLE_NODE_CONFIG_UPDATE = "node/config/update";

    public static final String ROLE_TOPIC_DATA_READ = "topic/data/read";
    public static final String ROLE_TOPIC_DATA_INSERT = "topic/data/insert";
    public static final String ROLE_TOPIC_DATA_DELETE = "topic/data/delete";

    public static final String ROLE_GROUP_READ = "group/read";
    public static final String ROLE_GROUP_DELETE = "group/delete";
    public static final String ROLE_GROUP_OFFSETS_UPDATE = "group/offsets/update";
    public static final String ROLE_GROUP_OFFSETS_DELETE = "group/offsets/delete";

    public static final String ROLE_ACLS_READ = "acls/read";

    public static final String ROLE_REGISTRY_READ = "registry/read";
    public static final String ROLE_REGISTRY_INSERT = "registry/insert";
    public static final String ROLE_REGISTRY_UPDATE = "registry/update";
    public static final String ROLE_REGISTRY_DELETE = "registry/delete";
    public static final String ROLE_REGISTRY_VERSION_DELETE = "registry/version/delete";

    public static final String ROLE_CONNECT_READ = "connect/read";
    public static final String ROLE_CONNECT_INSERT = "connect/insert";
    public static final String ROLE_CONNECT_UPDATE = "connect/update";
    public static final String ROLE_CONNECT_DELETE = "connect/delete";
    public static final String ROLE_CONNECT_STATE_UPDATE = "connect/state/update";
}
