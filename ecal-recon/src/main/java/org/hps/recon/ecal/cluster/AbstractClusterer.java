package org.hps.recon.ecal.cluster;

import java.util.List;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;

/**
 * This is an abstract class that {@link Clusterer} classes should implement
 * to perform a clustering algorithm on a <code>CalorimeterHit</code> collection.
 * The sub-class should implement {@link #createClusters(List)} which is 
 * the method that should perform the clustering algorithm.
 * 
 * @see Clusterer
 * @see org.lcsim.event.Cluster
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public abstract class AbstractClusterer implements Clusterer {
    
    private static Logger LOGGER = Logger.getLogger(AbstractClusterer.class.getPackage().getName());
    
    protected HPSEcal3 ecal;
    protected NeighborMap neighborMap;
    protected NumericalCuts cuts;
    
    /**
     * Default constructor which takes names of cuts and their default values.
     * These arguments cannot be null.  (Instead use the no-arg constructor.)
     * @param cutNames The names of the cuts for this clustering algorithm.
     * @param defaultCuts The default cut values for the algorithm matching the cutNames ordering.
     * @throw IllegalArgumentException if the arguments are null or the arrays are different lengths.
     */
    AbstractClusterer(String cutNames[], double[] defaultCuts) {
        if (cutNames == null) {
            throw new IllegalArgumentException("The cutNames is set to null.");
        }
        if (defaultCuts == null) {
            throw new IllegalArgumentException("The defaultCuts is set to null.");
        }
        if (cutNames.length != defaultCuts.length) {
            throw new IllegalArgumentException("The cutNames and defaultCuts are not the same length.");
        }
        cuts = new NumericalCutsImpl(cutNames, defaultCuts);
    }    
    
    /**
     * Constructor with cuts set.
     * @param cuts The numerical cuts.
     */
    AbstractClusterer(NumericalCuts cuts) {
        this.cuts = cuts;
    }
    
    /**
     * Default no-arg constructor.
     */
    AbstractClusterer() {
        // No cuts are set.  
    }
    
    /**
     * This is the primary method for sub-classes to implement their clustering algorithm.
     * @param hits
     * @return
     */
    public abstract List<Cluster> createClusters(EventHeader event, List<CalorimeterHit> hits);
    
    /**
     * Get the type code for the clusters produced by this algorithm.
     * @return The type code of the cluster.
     */
    public abstract ClusterType getClusterType();
    
    /**
     * Get the integer encoding of the <code>Cluster</code> type.
     * @return The integer encoding of the Cluster type.
     */
    public final int getClusterTypeEncoding() {
        return getClusterType().getType();
    }
    
    /**
     * Detector setup performed here to get reference to ECAL subdetector and neighbor mapping.
     */
    @Override
    public void conditionsChanged(ConditionsEvent event) {
        LOGGER.info("conditions change hook");
        
        // Default setup of ECAL subdetector.
        this.ecal = (HPSEcal3) DatabaseConditionsManager.getInstance().getDetectorObject().getSubdetector("Ecal");
        if (this.ecal == null) {
            throw new IllegalStateException("The ECal subdetector object is null");
        }
        this.neighborMap = ecal.getNeighborMap();
        if (this.neighborMap == null) {
            throw new IllegalStateException("The ECal neighbor map object is null");
        }
    }
    
    /**
     * By default nothing is done in this method, but start of job initialization can happen here like reading
     * cut settings into instance variables for convenience.  This is called in the <code>startOfData</code>
     * method of {@link ClusterDriver}.
     */
    public void initialize() {
    }    
          
    /**
     * Get the numerical cut settings.
     * @return The numerical cuts.
     */
    public final NumericalCuts getCuts() {
        return this.cuts;
    }
    
    /**
     * Convenience method to get the identifier helper from the ECAL subdetector.
     * @return The identifier helper.
     */
    protected final IIdentifierHelper getIdentifierHelper() {
        return ecal.getDetectorElement().getIdentifierHelper();
    }
    
    /**
     * Create a basic <code>Cluster</code> with the correct type.
     */
    public BaseCluster createBasicCluster() {
        BaseCluster cluster = new BaseCluster();
        cluster.setType(getClusterType().getType());
        return cluster;
    }
}
