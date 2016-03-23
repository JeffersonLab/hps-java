package org.hps.record.triggerbank;

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
     * Gets the number of the trigger which generated this object.
     * @return Returns either <code>0</code> or </code>1</code>.
     */
    public abstract int getTriggerNumber();
    
    /**
     * Indicates whether the trigger was reported by the trigger number
     * 0 trigger.
     * @return <code>true</code> if the trigger was reported by the
     * trigger number 0 trigger and <code>false</code> if by either
     * the trigger number 1 or an unknown trigger.
     */
    public abstract boolean isTrigger0();
    
    /**
     * Indicates whether the trigger was reported by the trigger number
     * 1 trigger.
     * @return <code>true</code> if the trigger was reported by the
     * trigger number 1 trigger and <code>false</code> if by either
     * the trigger number 0 or an unknown trigger.
     */
    public abstract boolean isTrigger1();
    
    
    /**
     * Indicates whether the trigger was reported by the first of the
     * singles triggers.
     * @return <code>true</code> if the trigger was reported by the
     * first trigger and <code>false</code> if it was reported by the
     * second trigger.
     */
    @Deprecated
    public abstract boolean isFirstTrigger();
    
    /**
     * Indicates whether the trigger was reported by the second of
     * the singles triggers.
     * @return <code>true</code> if the trigger was reported by the
     * second trigger and <code>false</code> if it was reported by
     * the first trigger.
     */
    @Deprecated
    public abstract boolean isSecondTrigger();
}