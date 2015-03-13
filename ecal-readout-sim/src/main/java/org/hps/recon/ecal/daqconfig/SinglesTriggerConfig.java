package org.hps.recon.ecal.daqconfig;

/**
 * Class <code>SinglesTriggerConfig</code> holds the configuration data
 * for a singles trigger.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class SinglesTriggerConfig extends AbstractConfig<AbstractConfig<Double>> {
	private static final int CUT_ENERGY_MIN = 0;
	private static final int CUT_ENERGY_MAX = 1;
	private static final int CUT_HIT_COUNT  = 2;
	
	/**
	 * Creates a new <code>SinglesTriggerConfig</code> object.
	 */
	SinglesTriggerConfig() {
		// Instantiate the base object.
		super(3);
		
		// Define the singles cuts.
		setValue(CUT_ENERGY_MIN, new LBOCutConfig());
		setValue(CUT_ENERGY_MAX, new UBOCutConfig());
		setValue(CUT_HIT_COUNT,  new LBOCutConfig());
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
}