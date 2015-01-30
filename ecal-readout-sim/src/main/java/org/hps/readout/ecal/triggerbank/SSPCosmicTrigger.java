package org.hps.readout.ecal.triggerbank;


/**
 * Class <code>SSPCosmicTrigger</code> represents an SSP trigger for
 * the either the top or bottom cosmic trigger.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class SSPCosmicTrigger extends SSPTrigger {
	/**
	 * Instantiates a new <code>SSPCosmicTrigger</code>.
	 * @param isTop - Indicates whether this trigger was caused by the
	 * top crate in the SSP or not.
	 * @param time - The time at which the trigger occurred.
	 */
	public SSPCosmicTrigger(boolean isTop, int time) {
		// Instantiate the superclass object.
		super(isTop ? SSPData.TRIG_TYPE_COSMIC_TOP : SSPData.TRIG_TYPE_COSMIC_BOT, time, 0);
	}
	
	/**
	 * Indicates whether the trigger was reported by the bottom SSP
	 * crate or not.
	 * @return Returns <code>true</code> if the trigger was reported
	 * by the bottom crate and <code>false</code> if it was reported
	 * by the top crate.
	 */
	public boolean isBottom() { return type == SSPData.TRIG_TYPE_COSMIC_BOT; }
	
	/**
	 * Indicates whether the trigger was reported by the top SSP
	 * crate or not.
	 * @return Returns <code>true</code> if the trigger was reported
	 * by the top crate and <code>false</code> if it was reported by
	 * the bottom crate.
	 */
	public boolean isTop() { return type == SSPData.TRIG_TYPE_COSMIC_TOP; }
}
