package org.hps.analysis.trigger.util;

import org.hps.record.triggerbank.TriggerModule2019;

public class SinglesTrigger2019<E, T> extends Trigger<E> {
    
    // Source of hodoscope
    private final T hodoSource;
    
    
    // Define the supported trigger cuts.
    private static final String CLUSTER_HIT_COUNT_LOW = TriggerModule2019.CLUSTER_HIT_COUNT_LOW;
    private static final String CLUSTER_TOTAL_ENERGY_LOW = TriggerModule2019.CLUSTER_TOTAL_ENERGY_LOW;
    private static final String CLUSTER_TOTAL_ENERGY_HIGH = TriggerModule2019.CLUSTER_TOTAL_ENERGY_HIGH;    
    private static final String CLUSTER_XMIN = TriggerModule2019.CLUSTER_XMIN;
    private static final String CLUSTER_PDE = "clusterPDE";
    private static final String HODO_L1_MATCHING = "hodoL1Matching";
    private static final String HODO_L2_MATCHING = "hodoL2Matching";
    private static final String HODO_L1L2_MATCHING = "hodoL1L2Matching";
    private static final String HODO_ECAL_MATCHING = "hodoEcalMatching";
    
    
    /**
     * Instantiates a new <code>SinglesTrigger</code> with all cut
     * states set to <code>false</code> and with the trigger source
     * defined according to the specified object.
     * @param source - The object from which the trigger cut states
     * are derived.
     */
    public SinglesTrigger2019(E source, T hodoSource, int triggerNum) {
        // Instantiate the superclass.
        super(source, triggerNum);
        
        this.hodoSource = hodoSource;
        
        // Add the supported cuts types.
        addValidCut(CLUSTER_HIT_COUNT_LOW);
        addValidCut(CLUSTER_TOTAL_ENERGY_LOW);
        addValidCut(CLUSTER_TOTAL_ENERGY_HIGH);
        addValidCut(CLUSTER_XMIN);
        addValidCut(CLUSTER_PDE);
        addValidCut(HODO_L1_MATCHING);
        addValidCut(HODO_L2_MATCHING);
        addValidCut(HODO_L1L2_MATCHING);
        addValidCut(HODO_ECAL_MATCHING);
    }
    
    /**
     * Gets the hodoscope object used for trigger.
     * @return Returns the trigger source object.
     */
    public T getHodoSource() { return hodoSource; }
    
    /**
     * Gets whether the cluster hit count cut was met.
     * @return Returns <code>true</code> if the cut was met and
     * <code>false</code> otherwise.
     */
    public boolean getStateHitCount() {
        return getCutState(CLUSTER_HIT_COUNT_LOW);
    }        
    
    /**
     * Gets whether the cluster total energy lower bound cut was met.
     * @return Returns <code>true</code> if the cut was met and
     * <code>false</code> otherwise.
     */
    public boolean getStateClusterEnergyLow() {
        return getCutState(CLUSTER_TOTAL_ENERGY_LOW);
    }
    
    /**
     * Gets whether the cluster total energy upper bound cut was met.
     * @return Returns <code>true</code> if the cut was met and
     * <code>false</code> otherwise.
     */
    public boolean getStateClusterEnergyHigh() {
        return getCutState(CLUSTER_TOTAL_ENERGY_HIGH);
    }
    
    /**
     * Gets whether both the cluster total energy upper and lower bound
     * cuts were met.
     * @return Returns <code>true</code> if the cuts were met and
     * <code>false</code> otherwise.
     */
    public boolean getStateClusterEnergy() {
        return getCutState(CLUSTER_TOTAL_ENERGY_LOW) && getCutState(CLUSTER_TOTAL_ENERGY_HIGH);
    }
    
    /**
     * Sets whether the conditions for the cluster hit count cut were
     * met.
     * @param state - <code>true</code> indicates that the cut conditions
     * were met and <code>false</code> that they were not.
     */
    public void setStateHitCount(boolean state) {
        setCutState(CLUSTER_HIT_COUNT_LOW, state);
    }    
    
    /**
     * Sets whether the conditions for the cluster total energy lower
     * bound cut were met.
     * @param state - <code>true</code> indicates that the cut conditions
     * were met and <code>false</code> that they were not.
     */
    public void setStateClusterEnergyLow(boolean state) {
        setCutState(CLUSTER_TOTAL_ENERGY_LOW, state);
    }
    
    /**
     * Sets whether the conditions for the cluster total energy upper
     * bound cut were met.
     * @param state - <code>true</code> indicates that the cut conditions
     * were met and <code>false</code> that they were not.
     */
    public void setStateClusterEnergyHigh(boolean state) {
        setCutState(CLUSTER_TOTAL_ENERGY_HIGH, state);
    }
    
    /**
     * Gets whether cluster xmin was met.
     * @return Returns <code>true</code> if the cut was met and
     * <code>false</code> otherwise.
     */
    public boolean getStateClusterXMin() {
        return getCutState(CLUSTER_XMIN);
    }
    
    /**
     * Sets whether the condition for cluster xmin was
     * met.
     * @param state - <code>true</code> indicates that the cut conditions
     * were met and <code>false</code> that they were not.
     */
    public void setStateClusterXMin(boolean state) {
        setCutState(CLUSTER_XMIN, state);
    }
        
    /**
     * Gets whether hodoscope L1 matching was met.
     * @return Returns <code>true</code> if the cut was met and
     * <code>false</code> otherwise.
     */
    public boolean getStateHodoL1Matching() {
        return getCutState(HODO_L1_MATCHING);
    }
    
    /**
     * Sets whether the condition for hodoscope L1 matching was
     * met.
     * @param state - <code>true</code> indicates that the cut conditions
     * were met and <code>false</code> that they were not.
     */
    public void setStateHodoL1Matching(boolean state) {
        setCutState(HODO_L1_MATCHING, state);
    }
    
    /**
     * Gets whether hodoscope L2 matching was met.
     * @return Returns <code>true</code> if the cut was met and
     * <code>false</code> otherwise.
     */
    public boolean getStateHodoL2Matching() {
        return getCutState(HODO_L2_MATCHING);
    }
    
    /**
     * Sets whether the condition for hodoscope L2 matching was
     * met.
     * @param state - <code>true</code> indicates that the cut conditions
     * were met and <code>false</code> that they were not.
     */
    public void setStateHodoL2Matching(boolean state) {
        setCutState(HODO_L2_MATCHING, state);
    }
    
    /**
     * Gets whether hodoscope L1 and L2 matching was met.
     * @return Returns <code>true</code> if the cut was met and
     * <code>false</code> otherwise.
     */
    public boolean getStateHodoL1L2Matching() {
        return getCutState(HODO_L1L2_MATCHING);
    }
    
    /**
     * Sets whether the condition for hodoscope L1 and L2 matching was
     * met.
     * @param state - <code>true</code> indicates that the cut conditions
     * were met and <code>false</code> that they were not.
     */
    public void setStateHodoL1L2Matching(boolean state) {
        setCutState(HODO_L1L2_MATCHING, state);
    }
    
    /**
     * Gets whether hodoscope and Ecal matching was met.
     * @return Returns <code>true</code> if the cut was met and
     * <code>false</code> otherwise.
     */
    public boolean getStateHodoEcalMatching() {
        return getCutState(HODO_ECAL_MATCHING);
    }
    
    /**
     * Sets whether the condition for hodoscope and Ecal matching was
     * met.
     * @param state - <code>true</code> indicates that the cut conditions
     * were met and <code>false</code> that they were not.
     */
    public void setStateHodoEcalMatching(boolean state) {
        setCutState(HODO_ECAL_MATCHING, state);
    }
    
    @Override
    public String toString() {
        return String.format("EClusterLow: %d; EClusterHigh %d; HitCount: %d; XMinCluster: %d; HodoL1Matching: %d; HodoL2Matching: %d; HodoL1L2Matching: %d; HodoEcalMatching: %d",
                getStateClusterEnergyLow() ? 1 : 0, getStateClusterEnergyHigh() ? 1 : 0,
                getStateHitCount() ? 1 : 0, getStateClusterXMin() ? 1 : 0, getStateHodoL1Matching() ? 1 : 0,
                        getStateHodoL2Matching() ? 1 : 0, getStateHodoL1L2Matching() ? 1 : 0, getStateHodoEcalMatching() ? 1 : 0);
    }
}