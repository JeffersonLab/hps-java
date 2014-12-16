package org.hps.recon.ecal.cluster;

import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;

/**
 * This is an abstract class that {@link Clusterer} classes should implement
 * to perform a clustering algorithm on a <code>CalorimeterHit</code> collection.
 * The sub-class should implement {@link #createClusters(List)} which is 
 * the method that should perform the clustering.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public abstract class AbstractClusterer implements Clusterer {
    
    protected HPSEcal3 ecal;
    protected NeighborMap neighborMap;
    protected double[] cuts;
    protected double[] defaultCuts;
    protected String[] cutNames;
    
    /**
     * This is the primary method for sub-classes to implement their clustering algorithm.
     * @param hits
     * @return
     */
    public abstract List<Cluster> createClusters(EventHeader event, List<CalorimeterHit> hits);
    
    /**
     * Detector setup performed here to get reference to ECAL subdetector and neighbor mapping.
     */
    @Override
    public void conditionsChanged(ConditionsEvent event) {
        // Default setup of ECAL subdetector.
        this.ecal = (HPSEcal3) DatabaseConditionsManager.getInstance().getDetectorObject().getSubdetector("Ecal");
        this.neighborMap = ecal.getNeighborMap();
    }
    
    /**
     * By default nothing is done in this method, but start of job initialization can happen here like reading
     * cut settings into instance variables for convenience.  This is called in the <code>startOfData</code>
     * method of {@link ClusterDriver}.
     */
    public void initialize() {
    }    
        
    /**
     * Default constructor which takes names of cuts and their default values.
     * Even if there are no cuts, these should be arrays of length 0 instead of null.
     * @param cutNames The names of the cuts for this clustering algorithm.
     * @param defaultCuts The default cut values for the algorithm matching the cutNames ordering.
     * @throw IllegalArgumentException if the arguments are null or the arrays are different lengths.
     */
    protected AbstractClusterer(String cutNames[], double[] defaultCuts) {
        if (cutNames == null) {
            throw new IllegalArgumentException("The cutNames is set to null.");
        }
        if (defaultCuts == null) {
            throw new IllegalArgumentException("The defaultCuts is set to null.");
        }
        if (cutNames.length != defaultCuts.length) {
            throw new IllegalArgumentException("The cutNames and defaultCuts are not the same length.");
        }
        this.cutNames = cutNames;
        this.defaultCuts = defaultCuts;
        this.cuts = defaultCuts;
    }
            
    public void setCuts(double[] cuts) {
        if (cuts.length != this.cutNames.length) {
            throw new IllegalArgumentException("The cuts array has the wrong length: " + cuts.length);
        }
        this.cuts = cuts;
    }
    
    public double[] getCuts() {
        return cuts;
    }
    
    public double getCut(String name) {
         int index = indexFromName(name);
         if (index == -1) {
             throw new IllegalArgumentException("There is no cut called " + name + " defined by this clusterer.");
         }
         return getCut(index);
    }
    
    public double getCut(int index) {
        if (index > cuts.length || index < 0) {
            throw new IndexOutOfBoundsException("The index " + index + " is out of bounds for cuts array.");
        }
        return cuts[index];
    }
         
    public String[] getCutNames() {
        return cutNames;
    }    
    
    @Override
    public void setCut(int index, double value) {
        cuts[index] = value;
    }

    public boolean isDefaultCuts() {
        return cuts == defaultCuts;
    }
    
    public double[] getDefaultCuts() {
        return defaultCuts;
    }
    
    public void setCut(String name, double value) {
        int index = indexFromName(name);
        cuts[index] = value;
    }
        
    protected int indexFromName(String name) {
        for (int index = 0; index < cuts.length; index++) {
            if (getCutNames()[index] == name) {
                return index;
            }                 
        }
        return -1;
    }       
}
