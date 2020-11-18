package org.hps.analysis.trigger.util;

import org.hps.record.triggerbank.TriggerModule2019;

public class PairTrigger2019<E> extends Trigger<E> {
    // Define the supported trigger cuts.
    private static final String CLUSTER_HIT_COUNT_LOW = TriggerModule2019.CLUSTER_HIT_COUNT_LOW;
    private static final String CLUSTER_TOTAL_ENERGY_LOW = TriggerModule2019.CLUSTER_TOTAL_ENERGY_LOW;
    private static final String CLUSTER_TOTAL_ENERGY_HIGH = TriggerModule2019.CLUSTER_TOTAL_ENERGY_HIGH;
    private static final String PAIR_ENERGY_SUM_LOW = TriggerModule2019.PAIR_ENERGY_SUM_LOW;
    private static final String PAIR_ENERGY_SUM_HIGH = TriggerModule2019.PAIR_ENERGY_SUM_HIGH;
    private static final String PAIR_ENERGY_DIFFERENCE_HIGH = TriggerModule2019.PAIR_ENERGY_DIFFERENCE_HIGH;
    private static final String PAIR_ENERGY_SLOPE_LOW = TriggerModule2019.PAIR_ENERGY_SLOPE_LOW;
    private static final String PAIR_COPLANARITY_HIGH = TriggerModule2019.PAIR_COPLANARITY_HIGH;
    private static final String PAIR_TIME_COINCIDENCE = TriggerModule2019.PAIR_TIME_COINCIDENCE;
    
    /**
     * Instantiates a new <code>PairTrigger</code> with all cut
     * states set to <code>false</code> and with the trigger source
     * defined according to the specified object.
     * @param source - The object from which the trigger cut states
     * are derived.
     */
    public PairTrigger2019(E source, int triggerNum) {
        // Instantiate the superclass.
        super(source, triggerNum);
        
        // Add the supported cuts types.
        addValidCut(CLUSTER_HIT_COUNT_LOW);
        addValidCut(CLUSTER_TOTAL_ENERGY_LOW);
        addValidCut(CLUSTER_TOTAL_ENERGY_HIGH);
        addValidCut(PAIR_ENERGY_SUM_LOW);
        addValidCut(PAIR_ENERGY_SUM_HIGH);
        addValidCut(PAIR_ENERGY_DIFFERENCE_HIGH);
        addValidCut(PAIR_ENERGY_SLOPE_LOW);
        addValidCut(PAIR_COPLANARITY_HIGH);
        addValidCut(PAIR_TIME_COINCIDENCE);
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
     * Gets whether the pair energy sum lower bound cut was met.
     * @return Returns <code>true</code> if the cut was met and
     * <code>false</code> otherwise.
     */
    public boolean getStateEnergySumLow() {
        return getCutState(PAIR_ENERGY_SUM_LOW);
    }
    
    /**
     * Gets whether the pair energy sum upper bound cut was met.
     * @return Returns <code>true</code> if the cut was met and
     * <code>false</code> otherwise.
     */
    public boolean getStateEnergySumHigh() {
        return getCutState(PAIR_ENERGY_SUM_HIGH);
    }
    
    /**
     * Gets whether both the pair energy sum upper and lower bound cuts
     * were met.
     * @return Returns <code>true</code> if the cuts were met and
     * <code>false</code> otherwise.
     */
    public boolean getStateEnergySum() {
        return getCutState(PAIR_ENERGY_SUM_LOW) && getCutState(PAIR_ENERGY_SUM_HIGH);
    }
    
    /**
     * Gets whether the pair energy difference cut was met.
     * @return Returns <code>true</code> if the cut was met and
     * <code>false</code> otherwise.
     */
    public boolean getStateEnergyDifference() {
        return getCutState(PAIR_ENERGY_DIFFERENCE_HIGH);
    }
    
    /**
     * Gets whether the pair energy slope cut was met.
     * @return Returns <code>true</code> if the cut was met and
     * <code>false</code> otherwise.
     */
    public boolean getStateEnergySlope() {
        return getCutState(PAIR_ENERGY_SLOPE_LOW);
    }
    
    /**
     * Gets whether the pair coplanarity cut was met.
     * @return Returns <code>true</code> if the cut was met and
     * <code>false</code> otherwise.
     */
    public boolean getStateCoplanarity() {
        return getCutState(PAIR_COPLANARITY_HIGH);
    }
    
    /**
     * Gets whether the time coincidence cut was met.
     * @return Returns <code>true</code> if the cut was met and
     * <code>false</code> otherwise.
     */
    public boolean getStateTimeCoincidence() {
        return getCutState(PAIR_TIME_COINCIDENCE);
    }
    
    /**
     * Sets whether the conditions for the pair energy sum lower bound
     * cut were met.
     * @param state - <code>true</code> indicates that the cut conditions
     * were met and <code>false</code> that they were not.
     */
    public void setStateEnergySumLow(boolean state) {
        setCutState(PAIR_ENERGY_SUM_LOW, state);
    }
    
    /**
     * Sets whether the conditions for the pair energy sum upper bound
     * cut were met.
     * @param state - <code>true</code> indicates that the cut conditions
     * were met and <code>false</code> that they were not.
     */
    public void setStateEnergySumHigh(boolean state) {
        setCutState(PAIR_ENERGY_SUM_HIGH, state);
    }
    
    /**
     * Sets whether the conditions for the pair energy difference cut
     * were met.
     * @param state - <code>true</code> indicates that the cut conditions
     * were met and <code>false</code> that they were not.
     */
    public void setStateEnergyDifference(boolean state) {
        setCutState(PAIR_ENERGY_DIFFERENCE_HIGH, state);
    }
    
    /**
     * Sets whether the conditions for the pair energy slope cut were
     * met.
     * @param state - <code>true</code> indicates that the cut conditions
     * were met and <code>false</code> that they were not.
     */
    public void setStateEnergySlope(boolean state) {
        setCutState(PAIR_ENERGY_SLOPE_LOW, state);
    }
    
    /**
     * Sets whether the conditions for the pair coplanarity cut were
     * met.
     * @param state - <code>true</code> indicates that the cut conditions
     * were met and <code>false</code> that they were not.
     */
    public void setStateCoplanarity(boolean state) {
        setCutState(PAIR_COPLANARITY_HIGH, state);
    }
    
    /**
     * Sets whether the conditions for the time coincidence cut were
     * met.
     * @param state - <code>true</code> indicates that the cut conditions
     * were met and <code>false</code> that they were not.
     */
    public void setStateTimeCoincidence(boolean state) {
        setCutState(PAIR_TIME_COINCIDENCE, state);
    }
    
    @Override
    public String toString() {
        return String.format("EClusterLow: %d; EClusterHigh %d; HitCount: %d; ESumLow: %d, ESumHigh: %d, EDiff: %d, ESlope: %d, Coplanarity: %d",
                getStateClusterEnergyLow() ? 1 : 0, getStateClusterEnergyHigh() ? 1 : 0,
                getStateHitCount() ? 1 : 0, getStateEnergySumLow() ? 1 : 0,
                getStateEnergySumHigh() ? 1 : 0, getStateEnergyDifference() ? 1 : 0,
                getStateEnergySlope() ? 1 : 0, getStateCoplanarity() ? 1 : 0);
    }
}