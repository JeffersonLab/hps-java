package org.hps.recon.ecal.triggerbank;

/**
 * Abstract class <code>SSPNumberedTrigger</code> is used as a base
 * class for any trigger types where there is more than one trigger
 * of that type in the SSP. It requires extending classes to support
 * the ability to return which trigger created the object.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see SSPTrigger
 */
public abstract class SSPNumberedTrigger extends SSPTrigger {
	/**
	 * Instantiates the <code>SSPNumberedTrigger</code>.
     * @param type - The type of trigger.
     * @param time - The time at which the trigger occurred in ns.
     * @param data - The trigger bit data.
	 */
	public SSPNumberedTrigger(int type, int time, int data) {
		super(type, time, data);
	}
	
    /**
     * Indicates whether the trigger was reported by the first of the
     * singles triggers.
     * @return <code>true</code> if the trigger was reported by the
     * first trigger and <code>false</code> if it was reported by the
     * second trigger.
     */
    public abstract boolean isFirstTrigger();
    
    /**
     * Indicates whether the trigger was reported by the second of
     * the singles triggers.
     * @return <code>true</code> if the trigger was reported by the
     * second trigger and <code>false</code> if it was reported by
     * the first trigger.
     */
    public abstract boolean isSecondTrigger();
}
