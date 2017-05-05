package org.hps.recon.tracking;

import java.util.List;

/**
 * An interface for finding clusters of RawTrackerHits on a sensor.
 */
public interface ClusteringAlgorithm {

    /**
     * Finds the clusters given a list of RawTrackerHits on a particular silicon sensor with
     * electrodes given by SiSensorElectrodes. A list of clusters is returned, with each cluster
     * being a list of RawTrackerHits the form the cluster.
     * 
     * @param hits base hits
     * @return list of clusters, with each cluster being a list of RawTrackerHits
     */
    public List<List<FittedRawTrackerHit>> findClusters(List<FittedRawTrackerHit> hits);

}