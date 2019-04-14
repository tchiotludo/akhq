package org.kafkahq.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.requests.DescribeLogDirsResponse;

@ToString
@EqualsAndHashCode
@Getter
public class LogDir {
    private final Integer brokerId;
    private final String path;
    private final String topic;
    private final int partition;
    private final long size;
    private final long offsetLag;
    private final boolean isFuture;

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
