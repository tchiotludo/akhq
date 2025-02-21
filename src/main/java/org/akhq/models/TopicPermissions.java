package org.akhq.models;

public record TopicPermissions(
    boolean create,
    boolean read,
    boolean update,
    boolean delete,
    boolean readConfig,
    boolean alterConfig,
    boolean topicDataRead,
    boolean topicDataCreate,
    boolean topicDataDelete,
    boolean consumerGroupRead
) {}
