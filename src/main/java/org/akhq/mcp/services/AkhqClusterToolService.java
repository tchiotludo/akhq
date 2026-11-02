package org.akhq.mcp.services;

import jakarta.inject.Singleton;
import org.akhq.controllers.AkhqController;
import org.akhq.mcp.model.ListClustersResult;

import java.util.List;

@Singleton
public class AkhqClusterToolService {
    private final AkhqController akhqController;

    public AkhqClusterToolService(AkhqController akhqController) {
        this.akhqController = akhqController;
    }

    /**
     * Lists the clusters the caller has a group on, as the UI cluster selector does.
     */
    public ListClustersResult listClusters() {
        List<ListClustersResult.ClusterOverview> clusters = akhqController.list().stream()
            .map(cluster -> new ListClustersResult.ClusterOverview(
                cluster.getId(),
                cluster.isRegistry(),
                cluster.getRegistryType(),
                cluster.getConnects(),
                cluster.getKsqldbs()
            ))
            .toList();

        return new ListClustersResult(clusters, clusters.isEmpty()
            ? "No cluster available for the current user."
            : "Found " + clusters.size() + " cluster(s). Use `name` as the `cluster` argument of the other tools.");
    }
}
