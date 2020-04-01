package org.akhq.service.mapper;

import org.akhq.models.Cluster;
import org.akhq.service.dto.ClusterDTO;

import javax.inject.Singleton;

@Singleton
public class ClusterMapper {

    public ClusterDTO fromCLusterToClusterDTO(Cluster cluster) {
        return new ClusterDTO(cluster.getId());
    }

}
