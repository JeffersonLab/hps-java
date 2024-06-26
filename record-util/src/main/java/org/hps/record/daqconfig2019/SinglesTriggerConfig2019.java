package org.hps.record.daqconfig2019;

/**
 * Class <code>SinglesTriggerConfig</code> holds the configuration data
 * for a singles trigger.
 */
public class SinglesTriggerConfig2019 extends AbstractConfig2019<AbstractConfig2019<Double>> {
    private static final int CUT_ENERGY_MIN = 0;
    private static final int CUT_ENERGY_MAX = 1;
    private static final int CUT_HIT_COUNT  = 2;
    private static final int CUT_X_MIN = 3;
    private static final int CUT_PDE = 4;
    private static final int L1_MATCHING = 5;
    private static final int L2_MATCHING = 6;
    private static final int L1L2_GEO_MATCHING = 7;
    private static final int HODOECAL_GEO_MATCHING = 8;
    
    //2021 update
    private static final int CUT_X_MAX = 9;
    private static final int CUT_Y_MIN = 10;
    private static final int CUT_Y_MAX = 11;    
    private boolean singlesMollerModeEnabled = false;
    private boolean singlesXYMinMaxEnabled = false;
    
    /**
     * Creates a new <code>SinglesTriggerConfig</code> object.
     */
    public SinglesTriggerConfig2019() {
        // Instantiate the base object.
        super(12);
        
        // Define the singles cuts.
        setValue(CUT_ENERGY_MIN, new LBOCutConfig2019());
        setValue(CUT_ENERGY_MAX, new UBOCutConfig2019());
        setValue(CUT_HIT_COUNT,  new LBOCutConfig2019());
        setValue(CUT_X_MIN,  new LBOCutConfig2019());
        setValue(CUT_PDE,  new PDECutConfig2019());
        setValue(L1_MATCHING,  new HodoEcalCoincidence2019());
        setValue(L2_MATCHING,  new HodoEcalCoincidence2019());
        setValue(L1L2_GEO_MATCHING,  new HodoEcalCoincidence2019());
        setValue(HODOECAL_GEO_MATCHING,  new HodoEcalCoincidence2019());
        
        //2021 update
        setValue(CUT_X_MAX,  new UBOCutConfig2019());
        setValue(CUT_Y_MIN,  new LBOCutConfig2019());
        setValue(CUT_Y_MAX,  new UBOCutConfig2019());
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
     * Gets the configuration object for the cluster XMin cut.
     * Note that cuts are in units of X.
     * @return Returns the configuration object for the cut.
     */
    public LBOCutConfig2019 getXMinCutConfig() {
        return (LBOCutConfig2019) getValue(CUT_X_MIN);
    }
    
    /**
     * Gets the configuration object for the cluster XMin cut.
     * Note that cuts are in units of X.
     * @return Returns the configuration object for the cut.
     */
    public PDECutConfig2019 getPDECutConfig() {
        return (PDECutConfig2019) getValue(CUT_PDE);
    }
    
    /**
     * Gets the configuration object for L1 matching.
     * @return Returns the configuration object.
     */
    public HodoEcalCoincidence2019 getL1MatchingConfig() {
        return (HodoEcalCoincidence2019) getValue(L1_MATCHING);
    }
    
    /**
     * Gets the configuration object for L2 matching.
     * @return Returns the configuration object.
     */
    public HodoEcalCoincidence2019 getL2MatchingConfig() {
        return (HodoEcalCoincidence2019) getValue(L2_MATCHING);
    }
    
    /**
     * Gets the configuration object for L1L2 geometry matching.
     * @return Returns the configuration object.
     */
    public HodoEcalCoincidence2019 getL1L2GeoMatchingConfig() {
        return (HodoEcalCoincidence2019) getValue(L1L2_GEO_MATCHING);
    }
    
    /**
     * Gets the configuration object for hodoscope and Ecal geometry matching.
     * @return Returns the configuration object.
     */
    public HodoEcalCoincidence2019 getHodoEcalGeoMatchingConfig() {
        return (HodoEcalCoincidence2019) getValue(HODOECAL_GEO_MATCHING);
    }
    
    //2021 update
    /**
     * Gets the configuration object for the cluster XMax cut.
     * Note that cuts are in units of X.
     * @return Returns the configuration object for the cut.
     */
    public UBOCutConfig2019 getXMaxCutConfig() {
        return (UBOCutConfig2019) getValue(CUT_X_MAX);
    }
    
    /**
     * Gets the configuration object for the cluster YMin cut.
     * Note that cuts are in units of Y.
     * @return Returns the configuration object for the cut.
     */
    public LBOCutConfig2019 getYMinCutConfig() {
        return (LBOCutConfig2019) getValue(CUT_Y_MIN);
    }
    
    /**
     * Gets the configuration object for the cluster YMax cut.
     * Note that cuts are in units of Y.
     * @return Returns the configuration object for the cut.
     */
    public UBOCutConfig2019 getYMaxCutConfig() {
        return (UBOCutConfig2019) getValue(CUT_Y_MAX);
    }
    
    /**
     * Sets whether singlesMollerModeEn is enabled
     * @param state <code>true</code> means that singlesMollerModeEn is enabled
     * and <code>false</code> that it is disabled.
     */
    protected void setIsSinglesMollerModeEnabled(boolean state) {
        singlesMollerModeEnabled = state;
    }
    
    /**
     * Indicates whether singlesMollerModeEn is enabled or not.
     * @return Returns <code>true</code> if singlesMollerModeEn is enabled and
     * <code>false</code> otherwise.
     */
    public boolean isSinglesMollerModeEnabled() {
        return singlesMollerModeEnabled;
    }
    
    /**
     * Sets whether singlesXYMinMaxEn is enabled
     * @param state <code>true</code> means that singlesXYMinMaxEn is enabled
     * and <code>false</code> that it is disabled.
     */
    protected void setIsSinglesXYMinMaxEnabled(boolean state) {
        singlesXYMinMaxEnabled = state;
    }
    
    /**
     * Indicates whether singlesXYMinMaxEn is enabled or not.
     * @return Returns <code>true</code> if singlesXYMinMaxEn is enabled and
     * <code>false</code> otherwise.
     */
    public boolean isSinglesXYMinMaxEnabled() {
        return singlesXYMinMaxEnabled;
    }
    
    
}
