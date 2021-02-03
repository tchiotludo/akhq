package org.akhq.models;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClusterStats
{
    private String id;
    private int partitions;
    private long replicaCount;
    private long inSyncReplicaCount;

}
