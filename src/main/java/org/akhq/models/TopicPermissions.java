package org.akhq.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class TopicPermissions {
    private boolean create;
    private boolean read;
    private boolean update;
    private boolean delete;
    private boolean readConfig;
    private boolean alterConfig;
    private boolean topicDataRead;
    private boolean topicDataCreate;
    private boolean topicDataDelete;
    private boolean consumerGroupRead;
}
