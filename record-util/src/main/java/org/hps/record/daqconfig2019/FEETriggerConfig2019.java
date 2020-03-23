package org.hps.record.daqconfig2019;


/**
 * Class <clode>FEETriggerConfig2019</code> holds the configuration data
 * for a FEE trigger
 * @author Tongtong Cao <caot@jlab.org>
 *
 */

public class FEETriggerConfig2019 extends AbstractConfig2019<AbstractConfig2019<Double>> {
    private static final int CUT_ENERGY_MIN = 0;
    private static final int CUT_ENERGY_MAX = 1;
    private static final int CUT_HIT_COUNT  = 2;
    private static final int FEE_PRESCALE_Region0 = 3;
    private static final int FEE_PRESCALE_Region1 = 4;
    private static final int FEE_PRESCALE_Region2 = 5;
    private static final int FEE_PRESCALE_Region3 = 6;
    private static final int FEE_PRESCALE_Region4 = 7;
    private static final int FEE_PRESCALE_Region5 = 8;
    private static final int FEE_PRESCALE_Region6 = 9;
    
    /**
     * Creates a new <code>FEETriggerConfig2019</code> object.
     */
    public FEETriggerConfig2019() {
        // Instantiate the base object.
        super(10);
        
        // Define the singles cuts.
        setValue(CUT_ENERGY_MIN, new LBOCutConfig2019());
        setValue(CUT_ENERGY_MAX, new UBOCutConfig2019());
        setValue(CUT_HIT_COUNT,  new LBOCutConfig2019());
        setValue(FEE_PRESCALE_Region0,  new FEEPrecaleConfig2019());
        setValue(FEE_PRESCALE_Region1,  new FEEPrecaleConfig2019());
        setValue(FEE_PRESCALE_Region2,  new FEEPrecaleConfig2019());
        setValue(FEE_PRESCALE_Region3,  new FEEPrecaleConfig2019());
        setValue(FEE_PRESCALE_Region4,  new FEEPrecaleConfig2019());
        setValue(FEE_PRESCALE_Region5,  new FEEPrecaleConfig2019());
        setValue(FEE_PRESCALE_Region6,  new FEEPrecaleConfig2019());
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
     * Gets the configuration object for the FEE prescale of region 0.
     * @return Returns the configuration object for the FEE prescale of region 0.
     */
    public FEEPrecaleConfig2019 getPrescaleRegion0Config() {
        return (FEEPrecaleConfig2019) getValue(FEE_PRESCALE_Region0);
    }
    
    /**
     * Gets the configuration object for the FEE prescale of region 1.
     * @return Returns the configuration object for the FEE prescale of region 1.
     */
    public FEEPrecaleConfig2019 getPrescaleRegion1Config() {
        return (FEEPrecaleConfig2019) getValue(FEE_PRESCALE_Region1);
    }
    
    /**
     * Gets the configuration object for the FEE prescale of region 2.
     * @return Returns the configuration object for the FEE prescale of region 2.
     */
    public FEEPrecaleConfig2019 getPrescaleRegion2Config() {
        return (FEEPrecaleConfig2019) getValue(FEE_PRESCALE_Region2);
    }
    
    /**
     * Gets the configuration object for the FEE prescale of region 3.
     * @return Returns the configuration object for the FEE prescale of region 3.
     */
    public FEEPrecaleConfig2019 getPrescaleRegion3Config() {
        return (FEEPrecaleConfig2019) getValue(FEE_PRESCALE_Region3);
    }
    
    /**
     * Gets the configuration object for the FEE prescale of region 4.
     * @return Returns the configuration object for the FEE prescale of region 4.
     */
    public FEEPrecaleConfig2019 getPrescaleRegion4Config() {
        return (FEEPrecaleConfig2019) getValue(FEE_PRESCALE_Region4);
    }
    
    /**
     * Gets the configuration object for the FEE prescale of region 5.
     * @return Returns the configuration object for the FEE prescale of region 5.
     */
    public FEEPrecaleConfig2019 getPrescaleRegion5Config() {
        return (FEEPrecaleConfig2019) getValue(FEE_PRESCALE_Region5);
    }
    
    /**
     * Gets the configuration object for the FEE prescale of region 6.
     * @return Returns the configuration object for the FEE prescale of region 6.
     */
    public FEEPrecaleConfig2019 getPrescaleRegion6Config() {
        return (FEEPrecaleConfig2019) getValue(FEE_PRESCALE_Region6);
    }
}
