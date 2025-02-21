package org.akhq.models;

public record Permissions(
    boolean create,
    boolean read,
    boolean update,
    boolean delete,
    boolean readConfig,
    boolean alterConfig,
    boolean topicDataRead,
    boolean topicDataCreate,
    boolean topicDataDelete
) {
    public Permissions() {
        this(
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false
        );
    }
}
