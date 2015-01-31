package org.hps.readout.ecal.triggerbank;


/**
 * Class <code>SSPSinglesTrigger</code> represents a singles trigger
 * reported by the SSP and also handles the parsing of the trigger data.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class SSPSinglesTrigger extends SSPTrigger {
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
    
    /**
     * Indicates whether the trigger was reported by the bottom SSP
     * crate or not.
     * @return Returns <code>true</code> if the trigger was reported
     * by the bottom crate and <code>false</code> if it was reported
     * by the top crate.
     */
    public boolean isBottom() {
        return (type == SSPData.TRIG_TYPE_SINGLES0_BOT || type == SSPData.TRIG_TYPE_SINGLES1_BOT);
    }
    
    /**
     * Indicates whether the trigger was reported by the top SSP
     * crate or not.
     * @return Returns <code>true</code> if the trigger was reported
     * by the top crate and <code>false</code> if it was reported by
     * the bottom crate.
     */
    public boolean isTop() {
        return (type == SSPData.TRIG_TYPE_SINGLES0_TOP || type == SSPData.TRIG_TYPE_SINGLES1_TOP);
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
}