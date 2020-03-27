package org.kafkahq.service.mapper;

import org.kafkahq.models.Cluster;
import org.kafkahq.service.dto.ClusterDTO;

import javax.inject.Singleton;

@Singleton
public class ClusterMapper {

    public ClusterDTO fromCLusterToClusterDTO(Cluster cluster) {
        return new ClusterDTO(cluster.getId());
    }

}
