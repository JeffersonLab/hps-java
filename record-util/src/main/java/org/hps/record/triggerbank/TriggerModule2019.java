package org.hps.record.triggerbank;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.hps.readout.util.HodoscopePattern;

import org.lcsim.event.Cluster;

public class TriggerModule2019 {
    /* Keys for general Cuts */
    
    /** The threshold for the cluster hit count cut. Clusters must have
     * this many hits or more to pass the cut. */
    public static final String CLUSTER_HIT_COUNT_LOW = "clusterHitCountLow";
    
    /** The lower bound for the cluster total energy cut. The energy
     * of a cluster must exceed this value to pass the cut. */
    public static final String CLUSTER_TOTAL_ENERGY_LOW = "clusterTotalEnergyLow";
    
    /** The upper bound for the cluster total energy cut. The energy
     * of a cluster must be below this value to pass the cut. Units are
     * in GeV. */
    public static final String CLUSTER_TOTAL_ENERGY_HIGH = "clusterTotalEnergyHigh";
    
    
    /* Keys for singles Cuts */
    public static final String CLUSTER_XMIN = "clusterXMIN";
    
    public static final String CLUSTER_PDE_C0 = "clusterPDEC0";
    
    public static final String CLUSTER_PDE_C1 = "clusterPDEC1";
    
    public static final String CLUSTER_PDE_C2 = "clusterPDEC2";
    
    public static final String CLUSTER_PDE_C3 = "clusterPDEC3";
    
    
    
    /** 
     * Stores the general cut values. 
     */
    private final Map<String, Double> generalCuts = new HashMap<String, Double>(3);
    
    
    /** 
     * Stores the singles cut values. 
     */
    private final Map<String, Double> singlesTriggerCuts = new HashMap<String, Double>(3);
    
    /**
     * Creates a default <code>TriggerModule</code>. The cuts are
     * defined such that all physical clusters (i.e. with energy
     * above zero) will be accepted.
     */
    public TriggerModule2019() {
        generalCuts.put(CLUSTER_HIT_COUNT_LOW, 0.0);
        generalCuts.put(CLUSTER_TOTAL_ENERGY_LOW, 0.0);
        generalCuts.put(CLUSTER_TOTAL_ENERGY_HIGH, Double.MAX_VALUE);        

        singlesTriggerCuts.put(CLUSTER_XMIN, 0.0);
        singlesTriggerCuts.put(CLUSTER_PDE_C0, 0.0);
        singlesTriggerCuts.put(CLUSTER_PDE_C1, 0.0);
        singlesTriggerCuts.put(CLUSTER_PDE_C2, 0.0);
        singlesTriggerCuts.put(CLUSTER_PDE_C3, 0.0);
        
    }
    
    /**
     * Get the value of the cut specified by the identifier given in
     * the argument to the specified value.
     * @param cut - The identifier of the cut to which the new value
     * should be assigned. These can be obtained as publicly-accessible
     * static variables from this class.
     * @return the value of the cut specified by the identifier given in
     * the argument to the specified value.
     * @throws IllegalArgumentException if the argument cut
     * identifier is not valid.
     */
    public double getCutValue(String cut) throws IllegalArgumentException {
        // Make sure that the cut exists. If it does, change it to the
        // new cut value.
        if(generalCuts.containsKey(cut)) {
            return generalCuts.get(cut);
        }
        else if(singlesTriggerCuts.containsKey(cut)) {
            return singlesTriggerCuts.get(cut);
        }
        
        // Otherwise, throw an exception.
        else { throw new IllegalArgumentException(String.format("Cut \"%s\" does not exist.", cut)); }
    }
    
    
    /**
     * Sets the value of the cut specified by the identifier given in
     * the argument to the specified value.
     * @param cut - The identifier of the cut to which the new value
     * should be assigned. These can be obtained as publicly-accessible
     * static variables from this class.
     * @param value - The new cut value.
     * @throws IllegalArgumentException Occurs if the argument cut
     * identifier is not valid.
     */
    public void setCutValue(String cut, double value) throws IllegalArgumentException {
        // Make sure that the cut exists. If it does, change it to the
        // new cut value.
        if(generalCuts.containsKey(cut)) {
            generalCuts.put(cut, value);
        }
        else if(singlesTriggerCuts.containsKey(cut)) {
            singlesTriggerCuts.put(cut, value);
        }
        
        // Otherwise, throw an exception.
        else { throw new IllegalArgumentException(String.format("Cut \"%s\" does not exist.", cut)); }
    }
    
    /**
     * Checks whether a cluster passes the cluster hit count cut. This
     * is defined as <code>N_hits >= CLUSTER_HIT_COUNT_LOW</code>.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public boolean clusterHitCountCut(Cluster cluster) {
        return clusterHitCountCut(getValueClusterHitCount(cluster));
    }
    
    /**
     * Checks whether the argument hit count meets the minimum required
     * hit count.
     * @param hitCount - The number of hits in the cluster.
     * @return Returns <code>true</code> if the count passes the cut
     * and <code>false</code> if the cluster does not.
     */
    private boolean clusterHitCountCut(int hitCount) {
        return (hitCount >= generalCuts.get(CLUSTER_HIT_COUNT_LOW));
    }
    
    /**
     * Gets the value used for the cluster hit count cut. This is the
     * total number of hits included in the cluster.
     * @param cluster - The cluster from which the value should be
     * derived.
     * @return Returns the number of hits in the cluster.
     */
    public static int getValueClusterHitCount(Cluster cluster) {
        return cluster.getCalorimeterHits().size();
    }
    
    /**
     * Checks whether a cluster passes the cluster total energy cut.
     * This is defined as <code>CLUSTER_TOTAL_ENERGY_LOW <= E_cluster
     * <= CLUSTER_TOTAL_ENERGY_HIGH</code>.
     * @param cluster - The cluster to check.
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public boolean clusterTotalEnergyCut(Cluster cluster) {
        return clusterTotalEnergyCut(getValueClusterTotalEnergy(cluster));
    }
    
    /**
     * Checks whether the argument energy falls within the allowed
     * cluster total energy range.
     * @param clusterEnergy - The energy of the entire cluster.
     * @return Returns <code>true</code> if the energy passes the cut
     * and <code>false</code> if the cluster does not.
     */
    private boolean clusterTotalEnergyCut(double clusterEnergy) {
        return clusterTotalEnergyCutHigh(clusterEnergy) && clusterTotalEnergyCutLow(clusterEnergy);
    }
    
    /**
     * Checks whether the argument energy falls below the cluster total
     * energy upper bound cut.
     * @param clusterEnergy - The energy of the entire cluster.
     * @return Returns <code>true</code> if the energy passes the cut
     * and <code>false</code> if the cluster does not.
     */
    private boolean clusterTotalEnergyCutHigh(double clusterEnergy) {
        return (clusterEnergy <= generalCuts.get(CLUSTER_TOTAL_ENERGY_HIGH));
    }
    
    /**
     * Checks whether the argument energy falls above the cluster total
     * energy lower bound cut.
     * @param clusterEnergy - The energy of the entire cluster.
     * @return Returns <code>true</code> if the energy passes the cut
     * and <code>false</code> if the cluster does not.
     */
    private boolean clusterTotalEnergyCutLow(double clusterEnergy) {
        return (clusterEnergy >= generalCuts.get(CLUSTER_TOTAL_ENERGY_LOW));
    }
    
    /**
     * Gets the value used for the seed hit energy cut.
     * @param cluster - The cluster from which the value should be
     * derived.
     * @return Returns the cluster seed energy in GeV.
     */
    public static double getValueClusterSeedEnergy(Cluster cluster) {
        return cluster.getCalorimeterHits().get(0).getCorrectedEnergy();
    }
    
    /**
     * Gets the number of hits in a cluster, as used in the cluster
     * hit count cut.
     * @param cluster - The cluster for which to obtain the size.
     * @return Returns the size as an <code>int</code>.
     */
    public static final double getClusterHitCount(Cluster cluster) {
        return cluster.getCalorimeterHits().size();
    }
    
    /**
     * Gets the value used for the cluster total energy cut. This is
     * the energy of the entire cluster.
     * @param cluster - The cluster from which the value should be
     * derived.
     * @return Returns the cluster energy in GeV.
     */
    public static double getValueClusterTotalEnergy(Cluster cluster) {
        return cluster.getEnergy();
    }    
    
    /**
     * Checks whether a cluster passes XMin cut. XMin is at least 0, i.e., located in positive part
     * @param x coordinate for a cluster
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public boolean clusterXMinCut(int x) {
        if(x < -22 || x > 23) throw new IllegalArgumentException(String.format("Parameter \"%d\" is out of X-coordinage range [1, 23].", x));
        else return (x >= singlesTriggerCuts.get(CLUSTER_XMIN));
    }
    
    /**
     * Checks whether a cluster passes position-dependent-energy cut passes.
     * @param cluster: a Ecal cluster
     * @param x: x coordinate of the cluster 
     * @param parametersPDE: 
     * @return Returns <code>true</code> if the cluster passes the cut
     * and <code>false</code> if the cluster does not.
     */
    public boolean clusterPDECut(Cluster cluster, int x) {
        if (x >= 1 && x < 23) return getValueClusterTotalEnergy(cluster) >= (singlesTriggerCuts.get(CLUSTER_PDE_C0) + singlesTriggerCuts.get(CLUSTER_PDE_C1) * x + singlesTriggerCuts.get(CLUSTER_PDE_C2) * Math.pow(x, 2) + singlesTriggerCuts.get(CLUSTER_PDE_C3) * Math.pow(x, 3));
        else if(x == 23) return true;
        else throw new IllegalArgumentException(String.format("Parameter \"%d\" is out of X-coordinage range [1, 23].", x));
    }
    
    /**
     * Checks whether geometry matching cut passes. Geometry matching includes hodo. L1L2 matching, Ecal-hodoL1 matching, and Ecal-hodoL2 matching
     * @param x: x-coordinate of Ecal cluster
     * @param y: y-coordinate of Ecal cluster
     * @param patternList: Patterns of hodoscope; Order of List: TopLayer1, TopLayer2, BotLayer1, BotLayer2
     * @return <code>true</code> if the geometry matching cut passes
     * and <code>false</code> if does not pass.
     */
    public boolean geometryMatchingCut(int x, int y, List<HodoscopePattern> patternList) {
        if(x < 1 || x > 23 || y == 0 || y < -5 || y > 5) throw new IllegalArgumentException(String.format("Parameter for x = %d is out of X-coordinate range [1, 23] or Parameter for y = %d out of Y-coordinate range [-5, -1] and [1, 5].", x, y));
        if(y > 0 && geometryHodoL1L2Matching(patternList.get(0), patternList.get(1)) && geometryEcalHodoMatching(x, patternList.get(0), patternList.get(1))) return true;
        if(y < 0 && geometryHodoL1L2Matching(patternList.get(2), patternList.get(3)) && geometryEcalHodoMatching(x, patternList.get(2), patternList.get(3))) return true;       
        
        return false;
    }
    
    /**
     * Checks whether geometry for hodo. L1L2 matches
     * @param layer1: layer1 at top/bot of hodo.
     * @param layer2: layer2 at top/bot of hodo.
     * @return <code>true</code> if geometry matches
     * and <code>false</code> if does not pass.
     */
    public boolean geometryHodoL1L2Matching(HodoscopePattern layer1, HodoscopePattern layer2){
        // Single tile hits    
        if(layer1.getHitStatus(HodoscopePattern.HODO_LX_1) && layer2.getHitStatus(HodoscopePattern.HODO_LX_1)) return true;
        if(layer1.getHitStatus(HodoscopePattern.HODO_LX_2) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_1) || layer2.getHitStatus(HodoscopePattern.HODO_LX_2))) return true;
        if(layer1.getHitStatus(HodoscopePattern.HODO_LX_3) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_2) || layer2.getHitStatus(HodoscopePattern.HODO_LX_3))) return true;
        if(layer1.getHitStatus(HodoscopePattern.HODO_LX_4) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_3) || layer2.getHitStatus(HodoscopePattern.HODO_LX_4))) return true;
        if(layer1.getHitStatus(HodoscopePattern.HODO_LX_5) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_4) || layer2.getHitStatus(HodoscopePattern.HODO_LX_5))) return true;
        // Clusters tile hits L1
        if(layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_12) && layer2.getHitStatus(HodoscopePattern.HODO_LX_1)) return true;
        if(layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_23) && layer2.getHitStatus(HodoscopePattern.HODO_LX_2)) return true;
        if(layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_34) && layer2.getHitStatus(HodoscopePattern.HODO_LX_3)) return true;
        if(layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_45) && layer2.getHitStatus(HodoscopePattern.HODO_LX_4)) return true;
        // Clusters tile hits L2
        if(layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_12) && (layer1.getHitStatus(HodoscopePattern.HODO_LX_2) || layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_12))) return true;
        if(layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_23) && layer1.getHitStatus(HodoscopePattern.HODO_LX_3)) return true;
        if(layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_34) && layer1.getHitStatus(HodoscopePattern.HODO_LX_4)) return true;
        if(layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_45) && layer1.getHitStatus(HodoscopePattern.HODO_LX_5)) return true;
                
        return false;
    }
    
    /**
     * Checks whether geometry for Ecal-hodoL1 matching, and Ecal-hodoL2 matches
     * @param x: x-coordinate of Ecal cluster
     * @param layer1: layer1 at top/bot of hodo.
     * @param layer2: layer2 at top/bot of hodo.
     * @return <code>true</code> if geometry matches
     * and <code>false</code> if does not pass.
     */
    public boolean geometryEcalHodoMatching(int x, HodoscopePattern layer1, HodoscopePattern layer2) { 
        if(x < 1 || x > 23) throw new IllegalArgumentException(String.format("Parameter \"%d\" is out of X-coordinage range [1, 23].", x));
        
        // Cluster X <-> Layer 1 Matching
        if((x >= 5) && (x <= 9) && (layer1.getHitStatus(HodoscopePattern.HODO_LX_1) || layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_12))) return true;
        if((x >= 6) && (x <= 11) && (layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_12) || layer1.getHitStatus(HodoscopePattern.HODO_LX_2) || layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_23))) return true;
        if((x >= 10) && (x <= 16) && (layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_23) || layer1.getHitStatus(HodoscopePattern.HODO_LX_3) || layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_34))) return true;
        if((x >= 15) && (x <= 21) && (layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_34) || layer1.getHitStatus(HodoscopePattern.HODO_LX_4) || layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_45))) return true;
        if((x >= 19) && (x <= 23) && (layer1.getHitStatus(HodoscopePattern.HODO_LX_CL_45) || layer1.getHitStatus(HodoscopePattern.HODO_LX_5))) return true;
        
        // Cluster X <-> Layer 2 Matching
        if((x >= 5) && (x <= 8) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_1) || layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_12))) return true;
        if((x >= 7) && (x <= 12) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_12) || layer2.getHitStatus(HodoscopePattern.HODO_LX_2) || layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_23))) return true;
        if((x >= 12) && (x <= 17) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_23) || layer2.getHitStatus(HodoscopePattern.HODO_LX_3) || layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_34))) return true;
        if((x >= 16) && (x <= 23) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_34) || layer2.getHitStatus(HodoscopePattern.HODO_LX_4) || layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_45))) return true;
        if((x >= 20) && (x <= 23) && (layer2.getHitStatus(HodoscopePattern.HODO_LX_CL_45) || layer2.getHitStatus(HodoscopePattern.HODO_LX_5))) return true;         
        
        return false;    
    }
    
}
