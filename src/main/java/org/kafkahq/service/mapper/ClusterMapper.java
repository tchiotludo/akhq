package org.kafkahq.service.mapper;

import org.kafkahq.configs.Connect;
import org.kafkahq.models.Cluster;
import org.kafkahq.service.dto.ClusterDTO;
import org.kafkahq.service.dto.ConnectDTO;

import javax.inject.Singleton;
import java.net.URL;

@Singleton
public class ClusterMapper {

    public ClusterDTO fromCLusterToClusterDTO(Cluster cluster) {
        return new ClusterDTO(cluster.getId());
    }

}
