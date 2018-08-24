package org.kafkahq.models;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.kafka.common.requests.DescribeLogDirsResponse;

@ToString
@EqualsAndHashCode
public class LogDir {
    public LogDir(Integer brokerId, String path, org.apache.kafka.common.TopicPartition topicPartition, DescribeLogDirsResponse.ReplicaInfo replicaInfo) {
        this.brokerId = brokerId;
        this.path = path;
        this.topic = topicPartition.topic();
        this.partition = topicPartition.partition();
        this.size = replicaInfo.size;
        this.offsetLag = replicaInfo.offsetLag;
        this.isFuture = replicaInfo.isFuture;
    }

    private final Integer brokerId;

    public Integer getBrokerId() {
        return brokerId;
    }

    /**
     * The absolute log directory path.
     */
    private final String path;

    public String getPath() {
        return path;
    }

    private final String topic;

    public String getTopic() {
        return topic;
    }

    private final int partition;

    public int getPartition() {
        return partition;
    }

    /**
     * The size of the log segments of the partition in bytes.
     */
    private final long size;

    public long getSize() {
        return size;
    }

    /**
     * The lag of the log's LEO w.r.t. partition's HW
     * (if it is the current log for the partition) or current replica's LEO
     * (if it is the future log for the partition)
     */
    private final long offsetLag;

    public long getOffsetLag() {
        return offsetLag;
    }

    /**
     * True if this log is created by AlterReplicaLogDirsRequest
     * and will replace the current log of the replica in the future.
     */
    private final boolean isFuture;

    public boolean isFuture() {
        return isFuture;
    }
}
