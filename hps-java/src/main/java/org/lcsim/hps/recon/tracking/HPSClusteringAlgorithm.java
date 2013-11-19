package org.lcsim.hps.recon.tracking;

import java.util.List;
import org.lcsim.event.base.BaseTrackerHit;

/**
 *
 * @author mgraham
 */


public interface HPSClusteringAlgorithm {

    /**
     * Finds the clusters given a list of RawTrackerHits on a particular
     * silicon sensor with electrodes given by SiSensorElectrodes.  A list
     * of clusters is returned, with each cluster being a list of RawTrackerHits
     * the form the cluster.
     *
     * @param hits base hits
     * @return list of clusters, with each cluster being a list of RawTrackerHits
     */
    public List<List<HPSFittedRawTrackerHit>> findClusters(
             List<HPSFittedRawTrackerHit> hits);

}