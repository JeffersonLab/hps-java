package org.hps.readout.ecal.triggerbank;


/**
 * Class <code>SSPPairTrigger</code> represents a pair trigger reported
 * by the SSP and also handles parsing the trigger bits.
 * 
 * @author Kyle McCarty <mccarty@jab.org>
 */
public class SSPPairTrigger extends SSPTrigger {
    /**
     * Instantiates a new <code>SSPPairTrigger</code>.
     * @param isFirstTrigger - Indicates whether the first or second
     * trigger produced this trigger.
     * @param time - The time at which the trigger occurred.
     * @param data - The trigger data associated with the trigger.
     */
    public SSPPairTrigger(boolean isFirstTrigger, int time, int data) {
        super(isFirstTrigger ? SSPData.TRIG_TYPE_PAIR0 : SSPData.TRIG_TYPE_PAIR1, time, data);
    }
    
    /**
     * Indicates whether the trigger was reported by the first of the
     * pair triggers.
     * @return <code>true</code> if the trigger was reported by the
     * first trigger and <code>false</code> if it was reported by the
     * second trigger.
     */
    public boolean isFirstTrigger() {
    	return (type == SSPData.TRIG_TYPE_PAIR0);
    }
    
    /**
     * Indicates whether the trigger was reported by the second of
     * the pair triggers.
     * @return <code>true</code> if the trigger was reported by the
     * second trigger and <code>false</code> if it was reported by
     * the first trigger.
     */
    public boolean isSecondTrigger() {
    	return (type == SSPData.TRIG_TYPE_PAIR1);
    }
    
    /**
     * Indicates whether the trigger passed the pair energy sum cut
     * or not.
     * @return Returns <code>true</code> if the cut passed and
     * <code>false</code> otherwise.
     */
    public boolean passCutEnergySum() {
        return (data & 1) == 1;
    }
    
    /**
     * Indicates whether the trigger passed the pair energy difference
     * cut or not.
     * @return Returns <code>true</code> if the cut passed and
     * <code>false</code> otherwise.
     */
    public boolean passCutEnergyDifference() {
        return ((data & 2) >> 1) == 1;
    }
    
    /**
     * Indicates whether the trigger passed the pair energy slope cut
     * or not.
     * @return Returns <code>true</code> if the cut passed and
     * <code>false</code> otherwise.
     */
    public boolean passCutEnergySlope() {
        return ((data & 4) >> 2) == 1;
    }
    
    /**
     * Indicates whether the trigger passed the pair coplanarity cut
     * or not.
     * @return Returns <code>true</code> if the cut passed and
     * <code>false</code> otherwise.
     */
    public boolean passCutCoplanarity() {
        return ((data & 8) >> 3) == 1;
    }
}