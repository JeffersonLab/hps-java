package org.hps.analysis.trigger.util;

import java.util.List;
import java.util.Map;

import org.hps.record.triggerbank.TriggerModule2019;
import org.lcsim.event.CalorimeterHit;
import org.hps.readout.util.HodoscopePattern;

public class SinglesTrigger2019<E> extends Trigger<E> {

    // hodoscope layers
    // layer1: 0
    // layer2: 1
    public static final int LAYER1 = 0;
    public static final int LAYER2 = 1;

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

    // Source of hodoscope
    private final List<CalorimeterHit> hodoHitList;

    // hodoscope pattern map
    private final Map<Integer, HodoscopePattern> patternMap;

    // indicate top or bot singles trigger
    private final int topnbot;

    /**
     * Instantiates a new <code>SinglesTrigger</code> with all cut states set to
     * <code>false</code> and with the trigger source defined according to the
     * specified object.
     * 
     * @param source - The object from which the trigger cut states are derived.
     */
    public SinglesTrigger2019(E source, List<CalorimeterHit> hodoHitList, Map<Integer, HodoscopePattern> patternMap,
            int triggerNum, int topnbot) {
        // Instantiate the superclass.
        super(source, triggerNum);

        this.hodoHitList = hodoHitList;

        this.patternMap = patternMap;

        this.topnbot = topnbot;

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
     * Get indicator to indicate singles trigger is top or bot
     * 
     * @return
     */
    public int getTopnbot() {
        return topnbot;
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
     * Get hodoscope pattern for a (y, layer) point.
     * 
     * @param a (y, layer) point
     * @return a hodoscope pattern
     */
    public HodoscopePattern getHodoPatternMap(int p) {
        return patternMap.get(p);
    }

    /**
     * Gets whether the cluster hit count cut was met.
     * 
     * @return Returns <code>true</code> if the cut was met and <code>false</code>
     *         otherwise.
     */
    public boolean getStateHitCount() {
        return getCutState(CLUSTER_HIT_COUNT_LOW);
    }

    /**
     * Gets whether the cluster total energy lower bound cut was met.
     * 
     * @return Returns <code>true</code> if the cut was met and <code>false</code>
     *         otherwise.
     */
    public boolean getStateClusterEnergyLow() {
        return getCutState(CLUSTER_TOTAL_ENERGY_LOW);
    }

    /**
     * Gets whether the cluster total energy upper bound cut was met.
     * 
     * @return Returns <code>true</code> if the cut was met and <code>false</code>
     *         otherwise.
     */
    public boolean getStateClusterEnergyHigh() {
        return getCutState(CLUSTER_TOTAL_ENERGY_HIGH);
    }

    /**
     * Gets whether both the cluster total energy upper and lower bound cuts were
     * met.
     * 
     * @return Returns <code>true</code> if the cuts were met and <code>false</code>
     *         otherwise.
     */
    public boolean getStateClusterEnergy() {
        return getCutState(CLUSTER_TOTAL_ENERGY_LOW) && getCutState(CLUSTER_TOTAL_ENERGY_HIGH);
    }

    /**
     * Sets whether the conditions for the cluster hit count cut were met.
     * 
     * @param state - <code>true</code> indicates that the cut conditions were met
     *              and <code>false</code> that they were not.
     */
    public void setStateHitCount(boolean state) {
        setCutState(CLUSTER_HIT_COUNT_LOW, state);
    }

    /**
     * Sets whether the conditions for the cluster total energy lower bound cut were
     * met.
     * 
     * @param state - <code>true</code> indicates that the cut conditions were met
     *              and <code>false</code> that they were not.
     */
    public void setStateClusterEnergyLow(boolean state) {
        setCutState(CLUSTER_TOTAL_ENERGY_LOW, state);
    }

    /**
     * Sets whether the conditions for the cluster total energy upper bound cut were
     * met.
     * 
     * @param state - <code>true</code> indicates that the cut conditions were met
     *              and <code>false</code> that they were not.
     */
    public void setStateClusterEnergyHigh(boolean state) {
        setCutState(CLUSTER_TOTAL_ENERGY_HIGH, state);
    }

    /**
     * Gets whether cluster PDE was met.
     * 
     * @return Returns <code>true</code> if the cut was met and <code>false</code>
     *         otherwise.
     */
    public boolean getStateClusterPDE() {
        return getCutState(CLUSTER_PDE);
    }

    /**
     * Sets whether the condition for cluster PDE was met.
     * 
     * @param state - <code>true</code> indicates that the cut conditions were met
     *              and <code>false</code> that they were not.
     */
    public void setStateClusterPDE(boolean state) {
        setCutState(CLUSTER_PDE, state);
    }

    /**
     * Gets whether cluster xmin was met.
     * 
     * @return Returns <code>true</code> if the cut was met and <code>false</code>
     *         otherwise.
     */
    public boolean getStateClusterXMin() {
        return getCutState(CLUSTER_XMIN);
    }

    /**
     * Sets whether the condition for cluster xmin was met.
     * 
     * @param state - <code>true</code> indicates that the cut conditions were met
     *              and <code>false</code> that they were not.
     */
    public void setStateClusterXMin(boolean state) {
        setCutState(CLUSTER_XMIN, state);
    }

    /**
     * Gets whether hodoscope L1 matching was met.
     * 
     * @return Returns <code>true</code> if the cut was met and <code>false</code>
     *         otherwise.
     */
    public boolean getStateHodoL1Matching() {
        return getCutState(HODO_L1_MATCHING);
    }

    /**
     * Sets whether the condition for hodoscope L1 matching was met.
     * 
     * @param state - <code>true</code> indicates that the cut conditions were met
     *              and <code>false</code> that they were not.
     */
    public void setStateHodoL1Matching(boolean state) {
        setCutState(HODO_L1_MATCHING, state);
    }

    /**
     * Gets whether hodoscope L2 matching was met.
     * 
     * @return Returns <code>true</code> if the cut was met and <code>false</code>
     *         otherwise.
     */
    public boolean getStateHodoL2Matching() {
        return getCutState(HODO_L2_MATCHING);
    }

    /**
     * Sets whether the condition for hodoscope L2 matching was met.
     * 
     * @param state - <code>true</code> indicates that the cut conditions were met
     *              and <code>false</code> that they were not.
     */
    public void setStateHodoL2Matching(boolean state) {
        setCutState(HODO_L2_MATCHING, state);
    }

    /**
     * Gets whether hodoscope L1 and L2 matching was met.
     * 
     * @return Returns <code>true</code> if the cut was met and <code>false</code>
     *         otherwise.
     */
    public boolean getStateHodoL1L2Matching() {
        return getCutState(HODO_L1L2_MATCHING);
    }

    /**
     * Sets whether the condition for hodoscope L1 and L2 matching was met.
     * 
     * @param state - <code>true</code> indicates that the cut conditions were met
     *              and <code>false</code> that they were not.
     */
    public void setStateHodoL1L2Matching(boolean state) {
        setCutState(HODO_L1L2_MATCHING, state);
    }

    /**
     * Gets whether hodoscope and Ecal matching was met.
     * 
     * @return Returns <code>true</code> if the cut was met and <code>false</code>
     *         otherwise.
     */
    public boolean getStateHodoEcalMatching() {
        return getCutState(HODO_ECAL_MATCHING);
    }

    /**
     * Sets whether the condition for hodoscope and Ecal matching was met.
     * 
     * @param state - <code>true</code> indicates that the cut conditions were met
     *              and <code>false</code> that they were not.
     */
    public void setStateHodoEcalMatching(boolean state) {
        setCutState(HODO_ECAL_MATCHING, state);
    }

    @Override
    public String toString() {
        return String.format(
                "EClusterLow: %d; EClusterHigh %d; HitCount: %d; XMinCluster: %d; HodoL1Matching: %d; HodoL2Matching: %d; HodoL1L2Matching: %d; HodoEcalMatching: %d",
                getStateClusterEnergyLow() ? 1 : 0, getStateClusterEnergyHigh() ? 1 : 0, getStateHitCount() ? 1 : 0,
                getStateClusterXMin() ? 1 : 0, getStateHodoL1Matching() ? 1 : 0, getStateHodoL2Matching() ? 1 : 0,
                getStateHodoL1L2Matching() ? 1 : 0, getStateHodoEcalMatching() ? 1 : 0);
    }
}