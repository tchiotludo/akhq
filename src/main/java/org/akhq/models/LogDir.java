package org.akhq.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.requests.DescribeLogDirsResponse;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class LogDir {
    private Integer brokerId;
    private String path;
    private String topic;
    private int partition;
    private long size;
    private long offsetLag;
    private boolean isFuture;

    public LogDir(Integer brokerId, String path, TopicPartition topicPartition, DescribeLogDirsResponse.ReplicaInfo replicaInfo) {
        this.brokerId = brokerId;
        this.path = path;
        this.topic = topicPartition.topic();
        this.partition = topicPartition.partition();
        this.size = replicaInfo.size;
        this.offsetLag = replicaInfo.offsetLag;
        this.isFuture = replicaInfo.isFuture;
    }
}
