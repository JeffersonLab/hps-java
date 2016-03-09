package org.hps.record.triggerbank;


/**
 * Class <code>SSPSinglesTrigger</code> represents a singles trigger
 * reported by the SSP and also handles the parsing of the trigger data.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class SSPSinglesTrigger extends SSPNumberedTrigger {
    /**
     * Instantiates a new <code>SSPSinglesTrigger</code> object.
     * @param isTrigger0 - Indicates whether this is the first or second
     * of the singles triggers.
     * @param isTop - Indicates whether this trigger was thrown by the
     * top or the bottom SSP crate.
     * @param time - Indicates at what time the trigger occurred in ns.
     * @param data - The raw trigger data associated with this trigger.
     */
    public SSPSinglesTrigger(boolean isTrigger0, boolean isTop, int time, int data) {
        // Initialize the superclass object.
        super(isTop ?
                (isTrigger0 ? SSPData.TRIG_TYPE_SINGLES0_TOP : SSPData.TRIG_TYPE_SINGLES1_TOP) :
                (isTrigger0 ? SSPData.TRIG_TYPE_SINGLES0_BOT : SSPData.TRIG_TYPE_SINGLES1_BOT),
                time, data);
    }
    
    @Override
    public boolean isFirstTrigger() {
        return (type == SSPData.TRIG_TYPE_SINGLES0_BOT) || (type == SSPData.TRIG_TYPE_SINGLES0_TOP);
    }
    
    @Override
    public boolean isSecondTrigger() {
        return (type == SSPData.TRIG_TYPE_SINGLES1_BOT) || (type == SSPData.TRIG_TYPE_SINGLES1_TOP);
    }
    
    /**
     * Indicates whether the trigger passed the cluster total energy
     * lower bound cut or not.
     * @return Returns <code>true</code> if the cut passed and
     * <code>false</code> otherwise.
     */
    public boolean passCutEnergyMin() {
        return (data & 1) == 1;
    }
    
    /**
     * Indicates whether the trigger passed the cluster total energy
     * upper bound cut or not.
     * @return Returns <code>true</code> if the cut passed and
     * <code>false</code> otherwise.
     */
    public boolean passCutEnergyMax() {
        return ((data & 2) >> 1) == 1;
    }
    
    /**
     * Indicates whether the trigger passed the cluster hit count cut
     * or not.
     * @return Returns <code>true</code> if the cut passed and
     * <code>false</code> otherwise.
     */
    public boolean passCutHitCount() {
        return ((data & 4) >> 2) == 1;
    }
    
    @Override
    public String toString() {
        return String.format("Trigger %d :: %3d ns :: EClusterLow: %d; EClusterHigh %d; HitCount: %d",
                isFirstTrigger() ? 1 : 2, getTime(), passCutEnergyMin() ? 1 : 0,
                passCutEnergyMax() ? 1 : 0, passCutHitCount() ? 1 : 0);
    }
}