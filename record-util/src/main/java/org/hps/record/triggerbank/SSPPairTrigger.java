package org.hps.record.triggerbank;


/**
 * Class <code>SSPPairTrigger</code> represents a pair trigger reported
 * by the SSP and also handles parsing the trigger bits.
 * 
 * @author Kyle McCarty <mccarty@jab.org>
 */
public class SSPPairTrigger extends SSPNumberedTrigger {
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
    
    @Override
    public boolean isFirstTrigger() {
        return (type == SSPData.TRIG_TYPE_PAIR0);
    }
    
    @Override
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
    
    @Override
    public String toString() {
        return String.format("Trigger %d :: %3d ns :: ESum: %d, EDiff: %d, ESlope: %d, Coplanarity: %d",
                isFirstTrigger() ? 1 : 2, getTime(), passCutEnergySum() ? 1 : 0,
                passCutEnergyDifference() ? 1 : 0, passCutEnergySlope() ? 1 : 0,
                passCutCoplanarity() ? 1 : 0);
    }
}