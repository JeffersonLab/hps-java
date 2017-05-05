package org.hps.record.triggerbank;

import java.util.HashSet;
import java.util.Set;

import org.hps.record.triggerbank.AbstractIntData.IntBankDefinition;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;

/**
 * Provides bit mask checks for the trigger types defined in
 * <a href="https://confluence.slac.stanford.edu/display/hpsg/EVIO+Data+Format#EVIODataFormat-EVIOEventtypes-2015DataSet">EVIO Event types</a>.
 */
public enum TriggerType {
    
    /** LED or Cosmic trigger (sometimes called calibration also). */
    LED_COSMIC(28),
    /** Pair 0 trigger. */
    PAIRS0(26),
    /** Pair 1 trigger. */
    PAIRS1(27),
    /** Pulser triggered event. */
    PULSER(29),
    /** Single 0 trigger. */
    SINGLES0(24),
    /** Single 1 trigger. */
    SINGLES1(25);
    
    /**
     * The bit number.
     */
    private int bit;

    /**
     * Constructor with bit number which is used to shift the input TI bits.    
     * @param bit the bit number for shifting the TI bits
     */
    private TriggerType(final int bit) {
        this.bit = bit;
    }

    /**
     * Get the bit number.    
     * @return the bit number
     */
    public int getBit() {
        return this.bit;
    }
    
    /**
     * Return <code>true</code> if the bits match this mask.    
     * @param triggerBits the trigger bits from the TI bank
     * @return <code>true</code> if the bits match this mask
     */
    public boolean matches(final int triggerBits) {
        return ((triggerBits >> this.bit) & 1) == 1;
    }
    
    /**
     * Definition of the TI bank in the EVIO event.
     */
    private static IntBankDefinition TI_BANK = new IntBankDefinition(TIData.class, new int[] {0x2e, 0xe10a});
    
    /**
     * Get the applicable trigger types from the TI data in an EVIO event.
     * @param evioEvent the input EVIO event with the TI data
     * @return the set of matching trigger types for the event
     */
    public static Set<TriggerType> getTriggerTypes(EvioEvent evioEvent) {
        Set<TriggerType> matches = new HashSet<TriggerType>();
        BaseStructure tiBank = TI_BANK.findBank(evioEvent);
        if (tiBank != null) {
            int[] triggerData = tiBank.getIntData();
            if (triggerData != null) {
                for (TriggerType triggerType : TriggerType.values()) {
                    if (triggerType.matches(triggerData[0])) {
                        matches.add(triggerType);
                    }
                }
            }
        }
        return matches;
    }      
}