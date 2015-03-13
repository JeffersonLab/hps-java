package org.hps.recon.ecal.daqconfig;

/**
 * Class <code>PairTriggerConfig</code> holds the configuration data
 * for a pair trigger.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class PairTriggerConfig extends AbstractConfig<AbstractConfig<Double>> {
	private static final int CUT_ENERGY_MIN   = 0;
	private static final int CUT_ENERGY_MAX   = 1;
	private static final int CUT_HIT_COUNT    = 2;
	private static final int CUT_ENERGY_SUM   = 3;
	private static final int CUT_ENERGY_DIFF  = 4;
	private static final int CUT_ENERGY_SLOPE = 5;
	private static final int CUT_COPLANARITY  = 6;
	private static final int CUT_TIME_DIFF    = 7;
	
	/**
	 * Creates a new <code>PairTriggerConfig</code> object.
	 */
	PairTriggerConfig() {
		// Instantiate the superclass.
		super(8);
		
		// Define the pair cuts.
		setValue(CUT_ENERGY_MIN,   new LBOCutConfig());
		setValue(CUT_ENERGY_MAX,   new UBOCutConfig());
		setValue(CUT_HIT_COUNT,    new LBOCutConfig());
		setValue(CUT_ENERGY_SUM,   new ULBCutConfig());
		setValue(CUT_ENERGY_DIFF,  new UBOCutConfig());
		setValue(CUT_ENERGY_SLOPE, new ESBCutConfig());
		setValue(CUT_COPLANARITY,  new UBOCutConfig());
		setValue(CUT_TIME_DIFF,    new UBOCutConfig());
	}
	
	/**
	 * Gets the configuration object for the cluster energy lower bound
	 * cut. Note that cuts are in units of GeV.
	 * @return Returns the configuration object for the cut.
	 */
	public LBOCutConfig getEnergyMinCutConfig() {
		return (LBOCutConfig) getValue(CUT_ENERGY_MIN);
	}
	
	/**
	 * Gets the configuration object for the cluster energy upper bound
	 * cut. Note that cuts are in units of GeV.
	 * @return Returns the configuration object for the cut.
	 */
	public UBOCutConfig getEnergyMaxCutConfig() {
		return (UBOCutConfig) getValue(CUT_ENERGY_MAX);
	}
	
	/**
	 * Gets the configuration object for the cluster hit count cut.
	 * Note that cuts are in units of calorimeter hits.
	 * @return Returns the configuration object for the cut.
	 */
	public LBOCutConfig getHitCountCutConfig() {
		return (LBOCutConfig) getValue(CUT_HIT_COUNT);
	}
	
	/**
	 * Gets the configuration object for the pair energy sum cut. Note
	 * that cuts are in units of GeV.
	 * @return Returns the configuration object for the cut.
	 */
	public ULBCutConfig getEnergySumCutConfig() {
		return (ULBCutConfig) getValue(CUT_ENERGY_SUM);
	}
	
	/**
	 * Gets the configuration object for the pair energy difference
	 * cut. Note that cuts are in units of GeV.
	 * @return Returns the configuration object for the cut.
	 */
	public UBOCutConfig getEnergyDifferenceCutConfig() {
		return (UBOCutConfig) getValue(CUT_ENERGY_DIFF);
	}
	
	/**
	 * Gets the configuration object for the pair energy slope cut.
	 * Note that cuts are in units of GeV and mm.
	 * @return Returns the configuration object for the cut.
	 */
	public ESBCutConfig getEnergySlopeCutConfig() {
		return (ESBCutConfig) getValue(CUT_ENERGY_SLOPE);
	}
	
	/**
	 * Gets the configuration object for the pair coplanarity cut.
	 * Note that cuts are in units of degrees.
	 * @return Returns the configuration object for the cut.
	 */
	public UBOCutConfig getCoplanarityCutConfig() {
		return (UBOCutConfig) getValue(CUT_COPLANARITY);
	}
	
	/**
	 * Gets the configuration object for the pair time coincidence cut.
	 * Note that cuts are in units of nanoseconds.
	 * @return Returns the configuration object for the cut.
	 */
	public UBOCutConfig getTimeDifferenceCutConfig() {
		return (UBOCutConfig) getValue(CUT_TIME_DIFF);
	}
	
}