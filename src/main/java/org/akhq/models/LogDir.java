package org.akhq.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.admin.ReplicaInfo;

@ToString
@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
public class LogDir {
    private Integer brokerId;
    private String path;
    private String topic;
    private int partition;
    private long size;
    private long offsetLag;
    private boolean future;

    @JsonIgnore
    public LogDir(Integer brokerId, String path, TopicPartition topicPartition, ReplicaInfo replicaInfo) {
        this.brokerId = brokerId;
        this.path = path;
        if (topicPartition != null) {
            this.topic = topicPartition.topic();
            this.partition = topicPartition.partition();
        }
        if (replicaInfo != null) {
            this.size = replicaInfo.size();
            this.offsetLag = replicaInfo.offsetLag();
            this.future = replicaInfo.isFuture();
        }
    }
}
