package org.hps.record.evio;

import org.jlab.coda.jevio.BaseStructure;

/**
 * Convenience enum for representing bank tag values in HPS-formatted EVIO events.
 * <p>
 * Documentation of the bank tags and contents can be found at<br/>
 * <a href="https://confluence.slac.stanford.edu/display/hpsg/EVIO+Data+Format">EVIO Data Format</a>.
 *
 * @author Jeremy McCormick, SLAC
 */
public enum EvioBankTag {

    /** EPICS header bank. */
    EPICS_HEADER(57618),
    /** EPICS mother bank. */
    EPICS_MOTHER(129),
    /** EPICS string data bank. */
    EPICS_STRING(57620),
    /** Event ID bank containing the EVIO event number in the run. */
    EVENT_ID(0xC000),
    /** Head bank with run number, etc. */
    HEAD(0xe10F),
    /** Scaler data bank. */
    SCALERS(57621),
    /** Trigger configuration bank. */
    TRIGGER_CONFIG(0xE10E),
    /** TI trigger bank. */
    TI_TRIGGER(0xe10a);
    
    /**
     * The bank's tag value.
     */
    private int bankTag;

    /**
     * Create a new bank enum.
     *
     * @param bankTag the bank value (tag value in the bank's header)
     */
    private EvioBankTag(final int bankTag) {
        this.bankTag = bankTag;
    }

    /**
     * Find a bank matching the tag value by looking through a bank and all its child banks.
     *
     * @param startBank the starting bank
     * @return the first bank matching the tag or <code>null<code> if not found
     */    
    public BaseStructure findBank(final BaseStructure startBank) {
        BaseStructure foundBank = null;
        if (this.equals(startBank)) {
            foundBank = startBank;
        } else if (startBank.getChildrenList() != null) {
            for (final BaseStructure subBank : startBank.getChildrenList()) {
                foundBank = this.findBank(subBank);
                if (foundBank != null) {
                    break;
                }
            }
        }
        return foundBank;
    }    
    
    /**
     * Get the bank tag value.
     *
     * @return the bank tag value
     */
    public int getBankTag() {
        return bankTag;
    }

    /**
     * Return <code>true</code> if bank's tag matches this one.
     *
     * @param bank the EVIO data bank
     * @return <code>true</code> if bank's tag matches this one
     */
    public boolean equals(final BaseStructure bank) {
        return bank.getHeader().getTag() == bankTag;
    }

    /**
     * Return <code>true</code> if the tag value matches this one.
     *
     * @param bankTag the bank tag value
     * @return <code>true</code> if the bank tag value matches this one
     */
    public boolean equals(final int bankTag) {
        return bankTag == this.getBankTag();
    }
}
