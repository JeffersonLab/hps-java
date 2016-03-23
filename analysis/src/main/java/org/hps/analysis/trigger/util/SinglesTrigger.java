package org.hps.analysis.trigger.util;

import org.hps.record.triggerbank.TriggerModule;

public class SinglesTrigger<E> extends Trigger<E> {
    // Define the supported trigger cuts.
    private static final String CLUSTER_HIT_COUNT_LOW = TriggerModule.CLUSTER_HIT_COUNT_LOW;
    private static final String CLUSTER_SEED_ENERGY_LOW = TriggerModule.CLUSTER_SEED_ENERGY_LOW;
    private static final String CLUSTER_SEED_ENERGY_HIGH = TriggerModule.CLUSTER_SEED_ENERGY_HIGH;
    private static final String CLUSTER_TOTAL_ENERGY_LOW = TriggerModule.CLUSTER_TOTAL_ENERGY_LOW;
    private static final String CLUSTER_TOTAL_ENERGY_HIGH = TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH;
    
    /**
     * Instantiates a new <code>SinglesTrigger</code> with all cut
     * states set to <code>false</code> and with the trigger source
     * defined according to the specified object.
     * @param source - The object from which the trigger cut states
     * are derived.
     */
    public SinglesTrigger(E source, int triggerNum) {
        // Instantiate the superclass.
        super(source, triggerNum);
        
        // Add the supported cuts types.
        addValidCut(CLUSTER_HIT_COUNT_LOW);
        addValidCut(CLUSTER_SEED_ENERGY_LOW);
        addValidCut(CLUSTER_SEED_ENERGY_HIGH);
        addValidCut(CLUSTER_TOTAL_ENERGY_LOW);
        addValidCut(CLUSTER_TOTAL_ENERGY_HIGH);
    }
    
    /**
     * Gets whether the cluster hit count cut was met.
     * @return Returns <code>true</code> if the cut was met and
     * <code>false</code> otherwise.
     */
    public boolean getStateHitCount() {
        return getCutState(CLUSTER_HIT_COUNT_LOW);
    }
    
    /**
     * Gets whether the cluster seed energy lower bound cut was met.
     * @return Returns <code>true</code> if the cut was met and
     * <code>false</code> otherwise.
     */
    public boolean getStateSeedEnergyLow() {
        return getCutState(CLUSTER_SEED_ENERGY_LOW);
    }
    
    /**
     * Gets whether the cluster seed energy upper bound cut was met.
     * @return Returns <code>true</code> if the cut was met and
     * <code>false</code> otherwise.
     */
    public boolean getStateSeedEnergyHigh() {
        return getCutState(CLUSTER_SEED_ENERGY_HIGH);
    }
    
    /**
     * Gets whether both the cluster seed energy upper and lower bound
     * cuts were met.
     * @return Returns <code>true</code> if the cuts were met and
     * <code>false</code> otherwise.
     */
    public boolean getStateSeedEnergy() {
        return getCutState(CLUSTER_SEED_ENERGY_LOW) && getCutState(CLUSTER_SEED_ENERGY_HIGH);
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
     * Sets whether the conditions for the cluster seed energy lower
     * bound cut were met.
     * @param state - <code>true</code> indicates that the cut conditions
     * were met and <code>false</code> that they were not.
     */
    public void setStateSeedEnergyLow(boolean state) {
        setCutState(CLUSTER_SEED_ENERGY_LOW, state);
    }
    
    /**
     * Sets whether the conditions for the cluster seed energy upper
     * bound cut were met.
     * @param state - <code>true</code> indicates that the cut conditions
     * were met and <code>false</code> that they were not.
     */
    public void setStateSeedEnergyHigh(boolean state) {
        setCutState(CLUSTER_SEED_ENERGY_HIGH, state);
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
    
    @Override
    public String toString() {
        return String.format("EClusterLow: %d; EClusterHigh %d; HitCount: %d",
                getStateClusterEnergyLow() ? 1 : 0, getStateClusterEnergyHigh() ? 1 : 0,
                getStateHitCount() ? 1 : 0);
    }
}