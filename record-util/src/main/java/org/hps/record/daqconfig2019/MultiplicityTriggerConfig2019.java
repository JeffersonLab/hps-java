package org.hps.record.daqconfig2019;

/**
 * Class <clode>MultiplicityTriggerConfig2019</code> holds the configuration data
 * for a multiplicity trigger
 * 
 * @author Tongtong Cao <caot@jlab.org>
 *
 */

public class MultiplicityTriggerConfig2019 extends AbstractConfig2019<AbstractConfig2019<Double>> {
    private static final int CUT_ENERGY_MIN = 0;
    private static final int CUT_ENERGY_MAX = 1;
    private static final int CUT_HIT_COUNT  = 2;
    private static final int CUT_TOP_NCLUSTERS  = 3;
    private static final int CUT_BOT_NCLUSTERS  = 4;
    private static final int CUT_TOT_NCLUSTERS  = 5;
    private static final int CUT_TIME_DIFF  = 6;
    
    /**
     * Creates a new <code>MultiplicityTrigger2019</code> object.
     */
    public MultiplicityTriggerConfig2019() {
        // Instantiate the base object.
        super(7);
        
        // Define the singles cuts.
        setValue(CUT_ENERGY_MIN, new LBOCutConfig2019());
        setValue(CUT_ENERGY_MAX, new UBOCutConfig2019());
        setValue(CUT_HIT_COUNT,  new LBOCutConfig2019());
        setValue(CUT_TOP_NCLUSTERS,  new LBOCutConfig2019());
        setValue(CUT_BOT_NCLUSTERS,  new LBOCutConfig2019());
        setValue(CUT_TOT_NCLUSTERS,  new LBOCutConfig2019());
        setValue(CUT_TIME_DIFF,  new UBOCutConfig2019());
    }
    
    /**
     * Gets the configuration object for the cluster energy lower bound
     * cut. Note that cuts are in units of GeV.
     * @return Returns the configuration object for the cut.
     */
    public LBOCutConfig2019 getEnergyMinCutConfig() {
        return (LBOCutConfig2019) getValue(CUT_ENERGY_MIN);
    }
    
    /**
     * Gets the configuration object for the cluster energy upper bound
     * cut. Note that cuts are in units of GeV.
     * @return Returns the configuration object for the cut.
     */
    public UBOCutConfig2019 getEnergyMaxCutConfig() {
        return (UBOCutConfig2019) getValue(CUT_ENERGY_MAX);
    }
    
    /**
     * Gets the configuration object for the cluster hit count cut.
     * Note that cuts are in units of calorimeter hits.
     * @return Returns the configuration object for the cut.
     */
    public LBOCutConfig2019 getHitCountCutConfig() {
        return (LBOCutConfig2019) getValue(CUT_HIT_COUNT);
    }
    
    /**
     * Gets the configuration object for minimum of the number of clusters at top.
     * Note that cuts are in units of calorimeter clusters.
     * @return Returns the configuration object for the cut.
     */
    public LBOCutConfig2019 getNClusterTopCutConfig() {
        return (LBOCutConfig2019) getValue(CUT_TOP_NCLUSTERS);
    }
    
    /**
     * Gets the configuration object for minimum of the number of clusters at bot.
     * Note that cuts are in units of calorimeter clusters.
     * @return Returns the configuration object for the cut.
     */
    public LBOCutConfig2019 getNClusterBotCutConfig() {
        return (LBOCutConfig2019) getValue(CUT_BOT_NCLUSTERS);
    }
    
    /**
     * Gets the configuration object for minimum of the total number of clusters.
     * Note that cuts are in units of calorimeter clusters.
     * @return Returns the configuration object for the cut.
     */
    public LBOCutConfig2019 getNClusterTotCutConfig() {
        return (LBOCutConfig2019) getValue(CUT_TOT_NCLUSTERS);
    }
            
    /**
     * Gets the configuration object for the pair time coincidence cut.
     * Note that cuts are in units of nanoseconds.
     * @return Returns the configuration object for the cut.
     */
    public UBOCutConfig2019 getTimeDifferenceCutConfig() {
        return (UBOCutConfig2019) getValue(CUT_TIME_DIFF);
    }

}
