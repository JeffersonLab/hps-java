package org.hps.recon.tracking;

import java.util.List;

import org.lcsim.event.LCRelation;

/**
 * An interface for finding clusters of hits on a sensor.
 */
public interface ClusteringAlgorithm {

    /**
     * Finds clusters given a list of hits on sensor.
     * 
     * @param hits Collection of hits to cluster. Most algorithms will use 
     *      fitted hits which are persited as LCRelations between the 
     *      RawTrackerHit and the resulting fit parameters.
     * @return List of clusters, with each cluster being a list of LCRelations
     */
    public List< List< LCRelation > > findClusters(List< LCRelation > hits);

}
