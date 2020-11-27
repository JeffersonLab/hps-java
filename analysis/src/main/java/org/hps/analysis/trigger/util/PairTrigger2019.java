package org.hps.analysis.trigger.util;

import java.util.List;
import java.util.Map;

import org.hps.readout.util.HodoscopePattern;
import org.hps.record.triggerbank.TriggerModule2019;
import org.lcsim.event.CalorimeterHit;

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
    // Only pair3 trigger requires geometry matching for hodoscope and Ecal
    private static final String PAIR_HODO_L1L2_COINCIDENCE_TOP = "hodoL1L2Top";
    private static final String PAIR_HODO_L1L2_MATCHING_TOP = "hodoL1L2MatchingTop";
    private static final String PAIR_HODO_ECAL_MATCHING_TOP = "hodoEcalMatchingTop";
    private static final String PAIR_HODO_L1L2_COINCIDENCE_BOT = "hodoL1L2Bot";
    private static final String PAIR_HODO_L1L2_MATCHING_BOT = "hodoL1L2MatchingBot";
    private static final String PAIR_HODO_ECAL_MATCHING_BOT = "hodoEcalMatchingBot";
    
    // Source of hodoscope
    private List<CalorimeterHit> hodoHitList = null;
    // hodoscope pattern map
    private Map<Integer, HodoscopePattern> patternMap = null;
    
    /**
     * Instantiates a new <code>PairTrigger</code> with all cut
     * states set to <code>false</code> and with the trigger source
     * defined according to the specified object.
     * @param source - The object from which the trigger cut states
     * are derived.
     * @param triggerNum - The trigger type
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
        // Only pair3 trigger requires geometry matching for hodoscope and Ecal
        addValidCut(PAIR_HODO_L1L2_COINCIDENCE_TOP);
        addValidCut(PAIR_HODO_L1L2_MATCHING_TOP);
        addValidCut(PAIR_HODO_ECAL_MATCHING_TOP);
        addValidCut(PAIR_HODO_L1L2_COINCIDENCE_BOT);
        addValidCut(PAIR_HODO_L1L2_MATCHING_BOT);
        addValidCut(PAIR_HODO_ECAL_MATCHING_BOT);
    }
    
    /**
     * Instantiates a new <code>PairTrigger</code> with all cut
     * states set to <code>false</code> and with the trigger source
     * defined according to the specified object.
     * @param source - The object from which the trigger cut states
     * are derived.
     * @param hodoHitList - list of hodoscope hits
     * @param patternMap - map of hodoscope patterns
     * @param triggerNum - The trigger type
     */
    public PairTrigger2019(E source, List<CalorimeterHit> hodoHitList, Map<Integer, HodoscopePattern> patternMap, int triggerNum) {
        // Instantiate the superclass.
        super(source, triggerNum);
        
        this.hodoHitList = hodoHitList;

        this.patternMap = patternMap;
        
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
        // Only pair3 trigger requires geometry matching for hodoscope and Ecal
        addValidCut(PAIR_HODO_L1L2_COINCIDENCE_TOP);
        addValidCut(PAIR_HODO_L1L2_MATCHING_TOP);
        addValidCut(PAIR_HODO_ECAL_MATCHING_TOP);
        addValidCut(PAIR_HODO_L1L2_COINCIDENCE_BOT);
        addValidCut(PAIR_HODO_L1L2_MATCHING_BOT);
        addValidCut(PAIR_HODO_ECAL_MATCHING_BOT);
    }
    
    /**
     * Gets the hodoscope hit list used for trigger.
     * 
     * @return Returns the hodoscope hit list.
     */
    public List<CalorimeterHit> getHodoHitList() {
        return hodoHitList;
    }

    /**
     * Gets the hodoscope pattern map.
     * 
     * @return Returns the hodoscope pattern map.
     */
    public Map<Integer, HodoscopePattern> getHodoPatternMap() {
        return patternMap;
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
    
    /**
     * Gets whether hodoscope L1L2 coincidence was met.
     * 
     * @return Returns <code>true</code> if the cut was met and <code>false</code>
     *         otherwise.
     */
    public boolean getStateHodoL1L2CoincidenceTop() {
        return getCutState(PAIR_HODO_L1L2_COINCIDENCE_TOP);
    }

    /**
     * Sets whether the condition for hodoscope L1L2 coincidence was met.
     * 
     * @param state - <code>true</code> indicates that the cut conditions were met
     *              and <code>false</code> that they were not.
     */
    public void setStateHodoL1L2CoincidenceTop(boolean state) {
        setCutState(PAIR_HODO_L1L2_COINCIDENCE_TOP, state);
    }

    /**
     * Gets whether hodoscope L1 and L2 matching was met.
     * 
     * @return Returns <code>true</code> if the cut was met and <code>false</code>
     *         otherwise.
     */
    public boolean getStateHodoL1L2MatchingTop() {
        return getCutState(PAIR_HODO_L1L2_MATCHING_TOP);
    }

    /**
     * Sets whether the condition for hodoscope L1 and L2 matching was met.
     * 
     * @param state - <code>true</code> indicates that the cut conditions were met
     *              and <code>false</code> that they were not.
     */
    public void setStateHodoL1L2MatchingTop(boolean state) {
        setCutState(PAIR_HODO_L1L2_MATCHING_TOP, state);
    }

    /**
     * Gets whether hodoscope and Ecal matching was met.
     * 
     * @return Returns <code>true</code> if the cut was met and <code>false</code>
     *         otherwise.
     */
    public boolean getStateHodoEcalMatchingTop() {
        return getCutState(PAIR_HODO_ECAL_MATCHING_TOP);
    }

    /**
     * Sets whether the condition for hodoscope and Ecal matching was met.
     * 
     * @param state - <code>true</code> indicates that the cut conditions were met
     *              and <code>false</code> that they were not.
     */
    public void setStateHodoEcalMatchingTop(boolean state) {
        setCutState(PAIR_HODO_ECAL_MATCHING_TOP, state);
    }
    
    /**
     * Gets whether hodoscope L1L2 coincidence was met.
     * 
     * @return Returns <code>true</code> if the cut was met and <code>false</code>
     *         otherwise.
     */
    public boolean getStateHodoL1L2CoincidenceBot() {
        return getCutState(PAIR_HODO_L1L2_COINCIDENCE_BOT);
    }

    /**
     * Sets whether the condition for hodoscope L1L2 coincidence was met.
     * 
     * @param state - <code>true</code> indicates that the cut conditions were met
     *              and <code>false</code> that they were not.
     */
    public void setStateHodoL1L2CoincidenceBot(boolean state) {
        setCutState(PAIR_HODO_L1L2_COINCIDENCE_BOT, state);
    }

    /**
     * Gets whether hodoscope L1 and L2 matching was met.
     * 
     * @return Returns <code>true</code> if the cut was met and <code>false</code>
     *         otherwise.
     */
    public boolean getStateHodoL1L2MatchingBot() {
        return getCutState(PAIR_HODO_L1L2_MATCHING_BOT);
    }

    /**
     * Sets whether the condition for hodoscope L1 and L2 matching was met.
     * 
     * @param state - <code>true</code> indicates that the cut conditions were met
     *              and <code>false</code> that they were not.
     */
    public void setStateHodoL1L2MatchingBot(boolean state) {
        setCutState(PAIR_HODO_L1L2_MATCHING_BOT, state);
    }

    /**
     * Gets whether hodoscope and Ecal matching was met.
     * 
     * @return Returns <code>true</code> if the cut was met and <code>false</code>
     *         otherwise.
     */
    public boolean getStateHodoEcalMatchingBot() {
        return getCutState(PAIR_HODO_ECAL_MATCHING_BOT);
    }

    /**
     * Sets whether the condition for hodoscope and Ecal matching was met.
     * 
     * @param state - <code>true</code> indicates that the cut conditions were met
     *              and <code>false</code> that they were not.
     */
    public void setStateHodoEcalMatchingBot(boolean state) {
        setCutState(PAIR_HODO_ECAL_MATCHING_BOT, state);
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