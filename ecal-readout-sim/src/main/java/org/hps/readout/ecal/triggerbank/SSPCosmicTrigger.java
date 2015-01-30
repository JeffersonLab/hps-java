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
	
	@Override
	public boolean isBottom() { return type == SSPData.TRIG_TYPE_COSMIC_BOT; }
	
	@Override
	public boolean isTop() { return type == SSPData.TRIG_TYPE_COSMIC_TOP; }
}
